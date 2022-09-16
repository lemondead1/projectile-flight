#version 110

uniform sampler2D diffuseTexture;
uniform vec4 tint;

varying vec4 fragColor;
varying vec2 fragTexPos;

void main() {
    gl_FragColor = vec4(1, 1, 1, texture2D(diffuseTexture, fragTexPos).r) * fragColor * tint;
}
