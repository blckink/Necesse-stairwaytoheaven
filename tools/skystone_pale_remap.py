# Himmelsstein heller: Helligkeit -> Perl-Blau-Rampe, Alpha unveraendert.
import sys
from PIL import Image
RAMP=[(0x2a,0x33,0x4a),(0x4a,0x58,0x78),(0x8e,0x9f,0xbd),(0xb7,0xc4,0xda),(0xd3,0xdc,0xec),(0xee,0xf2,0xfa)]
SRC=[0x24,0x40,0x5c,0x7e,0x9a,0xb9]  # Stuetzstellen der alten Helligkeit
def lum(r,g,b): return 0.299*r+0.587*g+0.114*b
def m(c):
    r,g,b=c[:3]
    if g>r+25 and g>b+15: return c  # Moos/Gras bleibt
    L=lum(r,g,b)
    for i in range(len(SRC)-1):
        if L<=SRC[i+1] or i==len(SRC)-2:
            t=max(0,min(1,(L-SRC[i])/(SRC[i+1]-SRC[i])));a,b2=RAMP[i],RAMP[i+1]
            return tuple(round(a[k]+(b2[k]-a[k])*t) for k in range(3))+(c[3],)
for src,dst in zip(sys.argv[1::2],sys.argv[2::2]):
    im=Image.open(src).convert('RGBA'); px=im.load()
    for y in range(im.height):
        for x in range(im.width):
            if px[x,y][3]: px[x,y]=m(px[x,y])
    im.save(dst)
