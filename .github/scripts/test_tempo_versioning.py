"""Boundary and regression tests for Tempo release normalization."""

import unittest
from tempo_versioning import normalize_version


class TempoVersioningTest(unittest.TestCase):
    def test_user_requested_sequence(self):
        self.assertEqual((4, 10, 25), normalize_version(4, 9, 125))
        self.assertEqual((5, 0, 25), normalize_version(4, 10, 125))
        self.assertEqual((5, 2, 5), normalize_version(5, 0, 205))

    def test_patch_rollover(self):
        self.assertEqual((4, 9, 99), normalize_version(4, 9, 99))
        self.assertEqual((4, 10, 0), normalize_version(4, 9, 100))
        self.assertEqual((5, 0, 0), normalize_version(4, 10, 100))
        self.assertEqual((5, 0, 25), normalize_version(4, 11, 25))
        self.assertEqual((5, 0, 0), normalize_version(4, 10, 99 + 1))
        self.assertEqual((7, 4, 3), normalize_version(5, 15, 2_003))

    def test_reject_invalid_components(self):
        for value in [(-1, 0, 0), (4, -1, 0), (4, 1, -1)]:
            with self.assertRaises(ValueError):
                normalize_version(*value)
        with self.assertRaises(TypeError):
            normalize_version(4, 9.0, 100)


if __name__ == "__main__":
    unittest.main()
