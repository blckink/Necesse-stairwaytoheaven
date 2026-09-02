# art-inbox — drop sprites here

Upload a PNG here from anywhere, including the GitHub mobile app, and the
**Fix sprites** action fits it to the sprite it replaces and opens a pull
request. You do not have to resize it, cut the palette, or clean the alpha —
that is the whole point.

**Name the file after the sprite it replaces.** `gloomshade.png` replaces
`src/main/resources/mobs/gloomshade.png` and inherits its exact size. New art
drawn on a vanilla sheet is `<vanillaname>-new-<ourname>.png`. A file whose
name matches nothing is reported and left alone, because guessing which sprite
a file replaces is how art gets silently overwritten.

What happens to it:

1. **Fit** to the target's exact size. An integer upscale is downsampled by
   the most common colour of each block, which is lossless; a 2.67x render
   goes through a box average first.
2. **Palette** cut to 32 colours. A generated render carries 10 000-30 000; a
   shipped Necesse sheet carries 19-38, and this one pass is worth more than
   all the others.
3. **Alpha** hardened to fully in or fully out.
4. **2x2 snap**, ground splats only — vanilla's tone unit is a 2x2 block and
   never a lone pixel. Mob sheets are left alone so the silhouette stays sharp.

The inbox is cleared by the run. The fitted file lives in the sprite it
replaced, and the before/after previews are attached to the run.

Locally the same thing is:

```sh
python3 tools/inbox_fix.py            # report
python3 tools/inbox_fix.py --apply    # and write
```
