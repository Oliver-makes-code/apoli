package io.github.apace100.apoli.power;

import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.mixin.EyeHeightAccess;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.apoli.util.HudRender;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtLong;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FireProjectilePower extends ActiveCooldownPower {

    private final EntityType entityType;
    private final int projectileCount;
    private final int interval;
    private final int startDelay;
    private final float speed;
    private final float divergence;
    private final SoundEvent soundEvent;
    private final NbtCompound tag;

    private boolean isFiringProjectiles;
    private boolean finishedStartDelay;
    private int shotProjectiles;

    public FireProjectilePower(PowerType<?> type, LivingEntity entity, int cooldownDuration, HudRender hudRender, EntityType entityType, int projectileCount, int interval, int startDelay, float speed, float divergence, SoundEvent soundEvent, NbtCompound tag) {
        super(type, entity, cooldownDuration, hudRender, null);
        this.entityType = entityType;
        this.projectileCount = projectileCount;
        this.interval = interval;
        this.startDelay = startDelay;
        this.speed = speed;
        this.divergence = divergence;
        this.soundEvent = soundEvent;
        this.tag = tag;
        this.setTicking(true);
    }

    @Override
    public void onUse() {
        if(canUse()) {
            isFiringProjectiles = true;
            use();
        }
    }

    @Override
    public NbtElement toTag() {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("LastUseTime", lastUseTime);
        nbt.putInt("ShotProjectiles", shotProjectiles);
        nbt.putBoolean("FinishedStartDelay", finishedStartDelay);
        nbt.putBoolean("IsFiringProjectiles", isFiringProjectiles);
        return nbt;
    }

    @Override
    public void fromTag(NbtElement tag) {
        if(tag instanceof NbtLong) {
            lastUseTime = ((NbtLong)tag).longValue();
        }
        else {
            lastUseTime = ((NbtCompound)tag).getLong("LastUseTime");
            shotProjectiles = ((NbtCompound)tag).getInt("ShotProjectiles");
            finishedStartDelay = ((NbtCompound)tag).getBoolean("FinishedStartDelay");
            isFiringProjectiles = ((NbtCompound)tag).getBoolean("IsFiringProjectiles");
        }
    }

    public void tick() {
        if(isFiringProjectiles) {
            if(!finishedStartDelay && startDelay == 0) {
                finishedStartDelay = true;
            }
            if(!finishedStartDelay && (entity.getEntityWorld().getTime() - lastUseTime) % startDelay == 0) {
                finishedStartDelay = true;
                shotProjectiles += 1;
                if(shotProjectiles <= projectileCount) {
                    if(soundEvent != null) {
                        entity.world.playSound((PlayerEntity)null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, SoundCategory.NEUTRAL, 0.5F, 0.4F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
                    }
                    if(!entity.world.isClient) {
                        fireProjectile();
                    }
                }
                else {
                    shotProjectiles = 0;
                    finishedStartDelay = false;
                    isFiringProjectiles = false;
                }
            }
            else if(interval == 0 && finishedStartDelay) {
                if(soundEvent != null) {
                    entity.world.playSound((PlayerEntity)null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, SoundCategory.NEUTRAL, 0.5F, 0.4F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
                }
                if(!entity.world.isClient) {
                    for(; shotProjectiles < projectileCount; shotProjectiles++) {
                        fireProjectile();
                    }
                }
                shotProjectiles = 0;
                finishedStartDelay = false;
                isFiringProjectiles = false;
            }
            else if (finishedStartDelay && (entity.getEntityWorld().getTime() - lastUseTime) % interval == 0) {
                shotProjectiles += 1;
                if(shotProjectiles <= projectileCount) {
                    if(soundEvent != null) {
                        entity.world.playSound((PlayerEntity)null, entity.getX(), entity.getY(), entity.getZ(), soundEvent, SoundCategory.NEUTRAL, 0.5F, 0.4F / (entity.getRandom().nextFloat() * 0.4F + 0.8F));
                    }
                    if(!entity.world.isClient) {
                        fireProjectile();
                    }
                }
                else {
                    shotProjectiles = 0;
                    finishedStartDelay = false;
                    isFiringProjectiles = false;
                }
            }
        }
    }

    private void fireProjectile() {
        if(entityType != null) {
            Entity entity = entityType.create(this.entity.world);
            if(entity == null) {
                return;
            }
            Vec3d rotationVector = this.entity.getRotationVector();
            float yaw = this.entity.getYaw();
            float pitch = this.entity.getPitch();
            Vec3d spawnPos = this.entity.getPos().add(0, ((EyeHeightAccess) this.entity).callGetEyeHeight(this.entity.getPose(), this.entity.getDimensions(this.entity.getPose())), 0).add(rotationVector);
            entity.refreshPositionAndAngles(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), pitch, yaw);
            if(entity instanceof ProjectileEntity) {
                if(entity instanceof ExplosiveProjectileEntity) {
                    ExplosiveProjectileEntity explosiveProjectileEntity = (ExplosiveProjectileEntity)entity;
                    explosiveProjectileEntity.powerX = rotationVector.x * speed;
                    explosiveProjectileEntity.powerY = rotationVector.y * speed;
                    explosiveProjectileEntity.powerZ = rotationVector.z * speed;
                }
                ProjectileEntity projectile = (ProjectileEntity)entity;
                projectile.setOwner(this.entity);
                projectile.setVelocity(this.entity, pitch, yaw, 0F, speed, divergence);
            } else {
                float f = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
                float g = -MathHelper.sin(pitch * 0.017453292F);
                float h = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
                Vec3d vec3d = (new Vec3d(f, g, h)).normalize().add(this.entity.getRandom().nextGaussian() * 0.007499999832361937D * (double)divergence, this.entity.getRandom().nextGaussian() * 0.007499999832361937D * (double)divergence, this.entity.getRandom().nextGaussian() * 0.007499999832361937D * (double)divergence).multiply((double)speed);
                entity.setVelocity(vec3d);
                Vec3d entityVelo = this.entity.getVelocity();
                entity.setVelocity(entity.getVelocity().add(entityVelo.x, this.entity.isOnGround() ? 0.0D : entityVelo.y, entityVelo.z));
            }
            if(tag != null) {
                NbtCompound mergedTag = entity.writeNbt(new NbtCompound());
                mergedTag.copyFrom(tag);
                entity.readNbt(mergedTag);
            }
            this.entity.world.spawnEntity(entity);
        }
    }

    public static PowerFactory createFactory() {
        return new PowerFactory<>(Apoli.identifier("fire_projectile"),
            new SerializableData()
                .add("cooldown", SerializableDataTypes.INT)
                .add("count", SerializableDataTypes.INT, 1)
                .add("interval", SerializableDataTypes.INT, 0)
                .add("start_delay", SerializableDataTypes.INT, 0)
                .add("speed", SerializableDataTypes.FLOAT, 1.5F)
                .add("divergence", SerializableDataTypes.FLOAT, 1F)
                .add("sound", SerializableDataTypes.SOUND_EVENT, null)
                .add("entity_type", SerializableDataTypes.ENTITY_TYPE)
                .add("hud_render", ApoliDataTypes.HUD_RENDER, HudRender.DONT_RENDER)
                .add("tag", SerializableDataTypes.NBT, null)
                .add("key", ApoliDataTypes.BACKWARDS_COMPATIBLE_KEY, new Active.Key()),
            data ->
                (type, player) -> {
                    FireProjectilePower power = new FireProjectilePower(type, player,
                        data.getInt("cooldown"),
                        (HudRender)data.get("hud_render"),
                        (EntityType)data.get("entity_type"),
                        data.getInt("count"),
                        data.getInt("interval"),
                        data.getInt("start_delay"),
                        data.getFloat("speed"),
                        data.getFloat("divergence"),
                        (SoundEvent)data.get("sound"),
                        (NbtCompound)data.get("tag"));
                    power.setKey((Active.Key)data.get("key"));
                    return power;
                })
            .allowCondition();
    }
}
