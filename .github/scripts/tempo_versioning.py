"""Normalize Tempo's MAJOR.MINOR.PATCH with bounded carry-over.

New release tags use patch 0..99 and minor 0..10 (inclusive). This helper does
not rewrite historical releases and does not determine Android versionCode.
"""

PATCH_BASE = 100
MINOR_BASE = 11


def normalize_version(major: int, minor: int, patch: int) -> tuple[int, int, int]:
    """Carry completed blocks of 100 patches through minor, then major.

    Examples: 4.9.125 -> 4.10.25; 4.10.125 -> 5.0.25;
    5.0.205 -> 5.2.5.
    """
    if not all(isinstance(n, int) and not isinstance(n, bool) for n in (major, minor, patch)):
        raise TypeError("Tempo version components must be integers")
    if min(major, minor, patch) < 0:
        raise ValueError("Tempo version components cannot be negative")
    extra_minor, patch = divmod(patch, PATCH_BASE)
    minor += extra_minor
    extra_major, minor = divmod(minor, MINOR_BASE)
    major += extra_major
    return major, minor, patch
