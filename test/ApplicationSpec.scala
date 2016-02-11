import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "200 on proxyFile" in new WithApplication{
      route(FakeRequest(GET, "/proxyFile/conductr_1.1.0-beta.5_all.deb")) must beSome.which (status(_) == OK)
    }

  }
}
