def jitter(rng, color, shade=0, spread=5, alpha=255):
    return tuple(max(0, min(255, c + shade + rng.randint(-spread, spread))) for c in color) + (alpha,)


def mix(a, b, k):
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))
