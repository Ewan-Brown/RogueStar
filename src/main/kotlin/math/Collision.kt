package math

//https://dyn4j.org/2010/01/sat/
fun doCollide(p1 : List<Vector2>, p2: List<Vector2>) : Boolean {
    val projTest1 = projectionTest(p1, p2)
    if(!projTest1) {
        return false
    }
    val projTest2 = projectionTest(p2, p1)
    if(!projTest2) {
        return false
    }
    return true
}

private fun projectionTest(p1 : List<Vector2>, p2 : List<Vector2>) : Boolean {
    val axes = getAxes(p1)
    for (axis in axes) {
        val projection1 = getProjection(p1, axis)
        val projection2 = getProjection(p2, axis)
        if (!projection1.overlaps(projection2)) return false
    }
    return true
}

fun getAxes(vectors: List<Vector2>) : List<Vector2> {
    val axes = mutableListOf<Vector2>()
    for (i in vectors.indices) {
        val v1 = vectors[i]
        val v2 = if(i == vectors.lastIndex) vectors[0] else vectors[i + 1]
        val edge = v2 - v1
        val normal = edge.rightHandNormal()
        axes.add(normal)
    }
    return axes
}

data class Projection(val min: Double, val max: Double){
    fun overlaps(otherProjection: Projection) : Boolean {
        if(this.max < otherProjection.min || this.min > otherProjection.max){
            return false
        }else{
            return true
        }
    }
}

fun getProjection(p : List<Vector2>, axis: Vector2) : Projection {
    var min = axis.dot(p[0])
    var max = min

    for(v in p) {
        val projVal = axis.dot(v)
        if(projVal < min){
            min = projVal
        }else if(projVal > max){
            max = projVal
        }
    }

    return Projection(min, max)
}