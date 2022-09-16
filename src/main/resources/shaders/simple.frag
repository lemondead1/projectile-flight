#version 110

uniform vec4 tint;

varying vec4 vertColor;

void main() {
    gl_FragColor = vertColor * tint;
}
