package physics

interface ProjectileBlueprint<P: ProjectileEntity> {
    fun build() : P
}

class BulletBlueprint(val mass: Double, val size: Double) : ProjectileBlueprint<BulletProjectile>{
    override fun build(): BulletProjectile {
        val bullet = BulletProjectile(mass, size)
        return bullet
    }
}

class LaserBlueprint() : ProjectileBlueprint<LaserProjectile>{
    override fun build(): LaserProjectile {
        val bullet = LaserProjectile(1.0)
        return bullet
    }
}