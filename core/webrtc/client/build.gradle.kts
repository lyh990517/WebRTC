import com.example.build_logic.setNamespace

plugins {
    id("core")
}

android {
    setNamespace("core.webrtc.client")
}

dependencies {
    implementation(projects.core.webrtc.api)
    implementation(projects.core.webrtc.controller)
    implementation(projects.core.webrtc.common)
    implementation(projects.core.webrtc.signaling)

    implementation(libs.google.webrtc)
}
