import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.dyn4j.geometry.Vector2

class VectorSerializer : StdSerializer<Vector2>(Vector2::class.java){
    override fun serialize(vector: Vector2, jgen: JsonGenerator, p2: SerializerProvider) {
        jgen.writeStartObject()
        jgen.writeNumberField("x", vector.x)
        jgen.writeNumberField("y", vector.y)
        jgen.writeEndObject()
    }
}

class VectorDeserializer : StdDeserializer<Vector2>(Vector2::class.java){
    override fun deserialize(parser: JsonParser, p1: DeserializationContext?): Vector2 {
        val node: JsonNode = parser.codec.readTree(parser)
        val x: Double = node.get("x").asDouble()
        val y: Double = node.get("y").asDouble()
        return Vector2(x, y)
    }
}

class ComponentSerializer : StdSerializer<ComponentBlueprint>(ComponentBlueprint::class.java){
    override fun serialize(component: ComponentBlueprint, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeStartObject()
        jgen.writeNumberField("shape", component.shape)
        jgen.writeNumberField("scale", component.scale)
        jgen.writeObjectField("position", component.position)
        jgen.writeNumberField("rotation", component.rotation)
        jgen.writeStringField("type", component.type.toString())
        jgen.writeEndObject()
    }
}

class ComponentDeserializer() : StdDeserializer<ComponentBlueprint>(ComponentBlueprint::class.java){
    override fun deserialize(parser: JsonParser, p1: DeserializationContext): ComponentBlueprint {
        val node: JsonNode = parser.codec.readTree(parser)
        val shape = node.get("shape").asInt()
        val scale = node.get("scale").asDouble()
        val x = node.get("position").get("x").asDouble()
        val y = node.get("position").get("y").asDouble()
        val rotation = node.get("rotation").asInt()
        val type = node.get("type").asText()!!

        val position = Vector2(x, y)
        return ComponentBlueprint(shape, scale, position, rotation, Type.valueOf(type))
    }
}