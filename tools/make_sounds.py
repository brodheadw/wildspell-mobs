"""Synthesizes the Frozen Zombie's sounds as mono Ogg Vorbis (Minecraft only attenuates mono sounds).

crunch: stiff, frozen joints grinding and packed ice crumbling as it drags itself forward. Built from
many short noise grains, band-limited so there is no bright ring (no "spoon on a glass").
shatter: death, a heavy crunch breaking into a spill of ice fragments.

Needs numpy and soundfile. Run from the repo root: python3 tools/make_sounds.py
"""
import os

import numpy as np
import soundfile

RATE = 44100
OUT = "src/main/resources/assets/wildspellmobs/sounds/frozen_zombie"


def band(signal, low, high):
    """Brick-wall band-pass with soft edges, done in the frequency domain."""
    spectrum = np.fft.rfft(signal)
    freqs = np.fft.rfftfreq(len(signal), 1 / RATE)
    gain = 1 / (1 + (low / np.maximum(freqs, 1)) ** 4) / (1 + (freqs / high) ** 4)
    return np.fft.irfft(spectrum * gain, len(signal))


def grain(rng, length):
    n = max(8, int(RATE * length))
    return rng.standard_normal(n) * np.hanning(n)


def place(buf, clip, at):
    i = int(at * RATE)
    end = min(len(buf), i + len(clip))
    if i < end:
        buf[i:end] += clip[:end - i]


def crunch(seed):
    """A dragging, gritty crunch: dense grains swelling and fading, with a couple of dull pops."""
    rng = np.random.default_rng(seed)
    duration = rng.uniform(0.32, 0.45)
    buf = np.zeros(int(RATE * duration))
    for _ in range(rng.integers(60, 110)):
        at = rng.uniform(0, duration * 0.85)
        swell = np.sin(np.pi * at / duration) ** 0.7                   # grinding builds then eases off
        place(buf, grain(rng, rng.uniform(0.001, 0.006)) * rng.uniform(0.2, 1.0) * swell, at)
    for _ in range(rng.integers(1, 3)):                                 # joints giving way
        place(buf, grain(rng, rng.uniform(0.012, 0.025)) * 2.2, rng.uniform(0.02, duration * 0.6))
    return band(buf, 180, 2600)


def shatter(seed):
    rng = np.random.default_rng(seed)
    buf = np.zeros(int(RATE * 0.9))
    place(buf, grain(rng, 0.03) * 3.0, 0.0)                             # the body breaking
    for _ in range(160):                                                # fragments spilling and settling
        at = rng.exponential(0.18)
        if at < 0.85:
            place(buf, grain(rng, rng.uniform(0.001, 0.004)) * rng.uniform(0.2, 1.0) * np.exp(-at * 2.5), at)
    return band(buf, 150, 4000)


def write_ogg(name, samples):
    samples = samples / (np.max(np.abs(samples)) or 1.0) * 0.7
    fade = int(RATE * 0.015)
    samples[:fade // 3] *= np.linspace(0, 1, fade // 3)
    samples[-fade:] *= np.linspace(1, 0, fade)
    soundfile.write(os.path.join(OUT, name + ".ogg"), samples, RATE, format="OGG", subtype="VORBIS")


for old in os.listdir(OUT):
    os.remove(os.path.join(OUT, old))
for i in range(1, 5):
    write_ogg(f"crunch{i}", crunch(400 + i))
for i in range(1, 3):
    write_ogg(f"shatter{i}", shatter(500 + i))
print("sounds written")
