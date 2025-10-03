package math

import physics.PhysicsLayer

//https://dyn4j.org/2010/01/sat/

//TODO make sure to check for containment!
fun getCollisionMTV(s1 : List<Vector2>, s2: List<Vector2>) : Vector2? {
    val projTest1 = projectionTest(s1, s2)?.relativeMTV ?: return null
    val projTest2 = projectionTest(s2, s1)?.relativeMTV ?: return null

    println("projTest1 = $projTest1")
    println("projTest2 = $projTest2")

    //Important to flip one of these, as they are coming from opposing perspectives :)
    val mtv = if (projTest1.getMagnitude() < projTest2.getMagnitude() ) projTest1 * -1.0 else projTest2

    return mtv
}

data class ProjectionResult(val relativeMTV: Vector2?, val s1Projections: List<Projection>)
/**
 * Test for projection overlap across all of s1's edge normals, return MTV, and projections
 */
private fun projectionTest(s1 : List<Vector2>, s2 : List<Vector2>) : ProjectionResult? {
    val normals = getSideNormals(s1)
    var minimumOverlap: Vector2? = null;
    val projectionResults = mutableListOf<Projection>()
    for (normal in normals) {
        val projection1 = getProjection(s1, normal)
        val projection2 = getProjection(s2, normal)
        val overlap = getOverlap(projection1, projection2)
        projectionResults.add(projection1)
        if(overlap == null) {
            return null;
        }else if(overlap > 0 && (minimumOverlap == null || overlap < minimumOverlap.getMagnitude())) {
            minimumOverlap = normal * overlap;
        }

    }
    return ProjectionResult(minimumOverlap, projectionResults)
}

//Get the normals of all the edges of a polygon
fun getSideNormals(points: List<Vector2>) : List<Vector2> {
    val normals = mutableListOf<Vector2>()
    for (i in points.indices) {
        val v1 = points[i]
        val v2 = if(i == points.lastIndex) points[0] else points[i + 1]
        val edge = v2 - v1
        val normal = edge.leftHandNormal()
        normals.add(normal.normalize())
    }
    return normals
}

//Defines a 1D "projection"
data class Projection(val min: Double, val max: Double, val normal: Vector2){
    override fun toString(): String {
        return "{$min - $max}"
    }
}

//Calculate (signed) overlap of two projections
fun getOverlap(projection: Projection, otherProjection: Projection) : Double? {
    if(projection.max <= otherProjection.min){
        return null
    }else if(projection.min >= otherProjection.max){
        return null
    }else{
        return projection.max - otherProjection.min
    }
}

//Project polygon p along normal
fun getProjection(p : List<Vector2>, normal: Vector2) : Projection {
    var min = normal.dot(p[0])
    var max = min

    for(i in 1..<p.size) {
        val v = p[i]
        val projVal = normal.dot(v)
        if(projVal < min){
            min = projVal
        }else if(projVal > max){
            max = projVal
        }
    }

    return Projection(min, max, normal)
}