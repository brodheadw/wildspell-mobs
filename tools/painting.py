def jitter(rng, color, shade=0, spread=5, alpha=255):
    return tuple(max(0, min(255, c + shade + rng.randint(-spread, spread))) for c in color) + (alpha,)


def mix(a, b, k):
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


def ramp(palette, level):
    return palette[int(round(max(0.0, min(1.0, level)) * (len(palette) - 1)))]


def hash01(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    return ((v * 2654435761) & 0xFFFFFFFF) / 0xFFFFFFFF
