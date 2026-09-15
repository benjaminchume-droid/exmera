package studio.exmera.engine.security

import java.security.MessageDigest

data class PrivacyPolicy(val stripLocation:Boolean=true,val stripDeviceIdentifiers:Boolean=true,val stripSoftwareTags:Boolean=false)
object MetadataSanitizer { fun sanitize(metadata:Map<String,String>,policy:PrivacyPolicy=PrivacyPolicy()):Map<String,String>{val banned=buildSet{if(policy.stripLocation){add("GPSLatitude");add("GPSLongitude");add("Location")}if(policy.stripDeviceIdentifiers){add("Serial");add("DeviceId");add("AndroidId")}if(policy.stripSoftwareTags)add("Software")};return metadata.filterKeys{it !in banned}} }
object Integrity { fun sha256(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){ "%02x".format(it) } }
data class SecurityPolicy(val allowNetworkModels:Boolean=false,val requireVerifiedPackages:Boolean=true,val allowTelemetry:Boolean=false)
class SecurityGate(private val policy:SecurityPolicy=SecurityPolicy()){fun allowNetworkModelDownload()=policy.allowNetworkModels;fun requirePackageVerification()=policy.requireVerifiedPackages;fun telemetryAllowed()=policy.allowTelemetry}
