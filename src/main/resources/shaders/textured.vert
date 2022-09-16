#version 110

uniform mat4 transformMatrix;

attribute vec3 position;
attribute vec4 color;
attribute vec2 texPos;

varying vec4 fragColor;
varying vec2 fragTexPos;

void main() {
    fragColor = color;
    gl_Position = transformMatrix * vec4(position, 1);
    fragTexPos = texPos;
}
