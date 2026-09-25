I found and fixed the "static with shaders" half of this on macOS (NeoForge 1.21.1, Grassier Grass 1.4.5, Iris, Apple M-series).

**Cause:** under Iris, blade animation runs in `GrassComputeAnimator`, which needs OpenGL 4.3 compute shaders. macOS caps OpenGL at 4.1, so `ensureInitialized()` logs `compute unavailable; static grass under Iris shaderpacks` and the grass never moves.

**Fix:** everything the compute pass does is available in GL 4.1 through **transform feedback**, so I added a fallback path in the same class that runs the existing compute shader's logic as a vertex shader:

- The GLSL is derived from `COMPUTE_SOURCE` at runtime (so there's one copy of the animation): `inData[i]` reads become `texelFetch` on a `usamplerBuffer` bound to the input buffer (`R32UI` buffer texture), `gl_GlobalInvocationID.x` becomes `gl_VertexID`, and `writeUintAtByte` writes into a per-vertex `uint` array.
- Transform feedback rewrites whole vertices, so the shader first copies in the old vertex words and then overwrites position and normal as before. It emits one `flat out uint` varying per word of the drawn vertex stride (one program per stride, captured interleaved), and draws `GL_POINTS` with `GL_RASTERIZER_DISCARD`.
- For the old words, it reads the *other* VBO of each mesh's double-buffer pair (the one drawn last frame, not being written), so there's no feedback loop and no per-frame buffer copy. Only a mesh's first dispatch does a `glCopyBufferSubData` into a scratch buffer.
- It saves and restores the VAO, program, active texture and buffer-texture bindings, like the compute path does. The compute path is unchanged on machines that support it.

Tested on an M5 Max (GL 4.1 Metal): the generated shader compiles and links, blades animate, and color, UV, light and normal words survive intact. I've been playing with it with Complementary Reimagined + Euphoria Patches and the grass moves again.

Happy to share the full patched `GrassComputeAnimator` privately, since the mod's source isn't public. Just let me know where to send it.

(This doesn't address the "invisible without shaders" half; I haven't looked at the vanilla-renderer path.)
