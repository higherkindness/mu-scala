import io.circe._
import io.circe.generic.semiauto._
import routeguide.protocols._

object codecs {

  implicit val pointDecoder: Decoder[Point] = deriveDecoder[Point]
  implicit val pointEncoder: Encoder[Point] = deriveEncoder[Point]

  implicit val featureDecoder: Decoder[Feature] = deriveDecoder[Feature]
  implicit val featureEncoder: Encoder[Feature] = deriveEncoder[Feature]

  implicit val featureDBDecoder: Decoder[FeatureDatabase] = deriveDecoder[FeatureDatabase]
  implicit val featureDBEncoder: Encoder[FeatureDatabase] = deriveEncoder[FeatureDatabase]

}
