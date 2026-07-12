import java.io.File
import java.util.Base64

val keystoreFile = File("debug.keystore")
if (!keystoreFile.exists()) {
    val base64File = File("debug.keystore.base64")
    if (base64File.exists()) {
        val decodedBytes = Base64.getMimeDecoder().decode(base64File.readText())
        keystoreFile.writeBytes(decodedBytes)
        println("Decoded debug.keystore")
    }
}
