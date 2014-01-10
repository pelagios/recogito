package integration

import org.junit.runner.RunWith
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.test.FakeApplication
import play.api.test.Helpers
import play.api.test.PlaySpecification
import play.api.test.WithBrowser
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AnnotationSpec extends PlaySpecification {

  "Text Annotation UI" should {

    val appWithMemoryDatabase = FakeApplication(additionalConfiguration = inMemoryDatabase("test"))

    "not annotate leading blanks, even if selected" in new WithBrowser[HtmlUnitDriver](webDriver = Helpers.HTMLUNIT, app = appWithMemoryDatabase, port = Helpers.testServerPort) {

      browser.goTo("http://localhost:" + port + "/recogito/login")
      browser.pageSource must contain("PELAGIOS")
      browser.title must contain("PELAGIOS") // page has been loaded 

      browser.fill("#username").`with`("test_user")
      browser.fill("#password").`with`("secret123")

      browser.submit("#loginbutton")

      browser.pageSource must not contain ("Invalid username")

    }
  }

}

