package controllers

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class CTSSpec extends Specification {

  "CTSClient" should {
    
    "not return any errors" in {
      val client = new DefaultCTSClient()
      val tei = client.getPlaintext("http://perseids.org/annotsrc/urn:cts:greekLit:tlg0016.tlg001.perseus-grc1:1.1")
      println(tei)
      tei must not have size (0)
    }
    
  }
 
  
}

class DefaultCTSClient extends CTSClient