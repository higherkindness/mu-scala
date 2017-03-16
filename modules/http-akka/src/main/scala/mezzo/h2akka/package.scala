/* -
 * Mezzo [http-akka]
 */

package mezzo

package object h2akka {
  type AkkaHttpClient = Dehydrated {
    type H = HydrateAkkaHttpClient[_]
  }

  type AkkaHttpRoutes = Dehydrated {
    type H = HydrateAkkaHttpRoutes[_]
  }
}
