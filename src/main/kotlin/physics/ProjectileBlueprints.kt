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