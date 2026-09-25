package com.wildspell.mobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/** A small light-blue ice mote that tumbles downward, drifts, shrinks and fades; always fully lit. */
public class FrostMoteParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;

    FrostMoteParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.friction = 0.94F;
        this.gravity = 0.03F;
        this.lifetime = 18 + this.random.nextInt(18);
        this.startSize = this.quadSize = 0.05F + this.random.nextFloat() * 0.06F;
        float shade = 0.85F + this.random.nextFloat() * 0.15F;
        this.setColor(0.72F * shade, 0.92F * shade, 1.0F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            float life = (float) this.age / this.lifetime;
            this.quadSize = this.startSize * (1.0F - life * 0.7F);
            this.alpha = 1.0F - life * life;
            this.xd += (this.random.nextFloat() - 0.5F) * 0.004F;
            this.zd += (this.random.nextFloat() - 0.5F) * 0.004F;
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new FrostMoteParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
