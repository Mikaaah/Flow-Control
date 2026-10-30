"""Rebuild the pixel-aligned slate/copper GUI textures (requires Pillow)."""
from pathlib import Path
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/streamcontrol/textures/gui'
OUT.mkdir(parents=True, exist_ok=True)
NAVY = '#20293f'
TEAL = '#729aa1'
LIGHT = '#a9c3c8'
WHITE = '#fbfcf8'

def rect(im, box, color):
    ImageDraw.Draw(im).rectangle(box, fill=color)

def panel(im, x, y, w, h, copper=False, hover=False, disabled=False):
    d=ImageDraw.Draw(im)
    d.rounded_rectangle((x,y,x+w-1,y+h-1),radius=1,fill='#4d2b32' if copper else NAVY)
    edge='#c09473' if copper else TEAL
    d.rounded_rectangle((x+1,y+1,x+w-2,y+h-2),radius=1,fill=edge)
    a,b=((153,99,77),(122,72,65)) if copper else ((76,104,126),(59,84,108))
    if disabled:a,b=(52,71,89),(43,59,77)
    if hover:a=tuple(min(255,c+15) for c in a);b=tuple(min(255,c+12) for c in b)
    for row in range(h-4):
        t=row/max(1,h-5)
        c=tuple(round(a[i]*(1-t)+b[i]*t) for i in range(3))
        d.line((x+2,y+2+row,x+w-3,y+2+row),fill=c)
    d.line((x+2,y+1,x+w-3,y+1),fill='#d5ad8b' if copper else LIGHT)
    d.line((x+1,y+2,x+1,y+h-3),fill='#c09473' if copper else LIGHT)
    d.line((x+2,y+h-3,x+w-3,y+h-3),fill='#663b36' if copper else '#304358')
    d.line((x+w-3,y+3,x+w-3,y+h-3),fill='#663b36' if copper else '#354b61')

im=Image.new('RGBA',(316,238))
d=ImageDraw.Draw(im)
outline=[(8,14),(43,14),(43,2),(273,2),(273,14),(307,14),(312,19),(312,229),(307,234),(8,234),(3,229),(3,19)]
d.polygon([(x+1,y+2) for x,y in outline],fill=NAVY)
d.polygon(outline,fill=WHITE)
inner=[(9,17),(46,17),(46,5),(270,5),(270,17),(306,17),(309,20),(309,228),(306,231),(9,231),(6,228),(6,20)]
d.polygon(inner,fill=NAVY)
inside=[(10,18),(47,18),(47,6),(269,6),(269,18),(305,18),(308,21),(308,227),(305,230),(10,230),(7,227),(7,21)]
d.polygon(inside,fill='#446077')
d.line([(9,21),(9,226),(11,228)],fill='#304358')
d.line([(10,19),(45,19)],fill=LIGHT)
d.line([(272,19),(304,19),(307,22)],fill=LIGHT)
# Raised title plate, inset copper cards and a navy basin.
panel(im,49,7,218,18)
panel(im,13,48,108,122,True)
panel(im,127,48,177,122,True)
panel(im,19,65,88,81)
rect(im,(22,68,103,139),'#20293f')
# Fine stable material marks; their low contrast leaves text readable.
for i in range(105):
    x=16+(i*43+9)%102;y=51+(i*29+3)%115
    if not (19<=x<107 and 65<=y<146):rect(im,(x,y,x,y),'#95624d' if i%2 else '#875442')
for i in range(82):
    x=131+(i*53+7)%168;y=51+(i*31+11)%115
    rect(im,(x,y,x,y),'#95624d' if i%2 else '#875442')
# Accent joints and inset line beneath the heading.
d.line((46,40,300,40),fill='#304358');d.line((46,41,300,41),fill=TEAL)
for x in (14,299):
    rect(im,(x,23,x+2,25),NAVY);rect(im,(x,23,x+1,24),LIGHT)
im.save(OUT/'controller.png')
for name,w,h in [('toggle',286,22),('apply',174,20),('cancel',104,20),('compass',26,14),('handle',9,14)]:
    atlas=Image.new('RGBA',(w,h*5))
    for state in range(5):
        panel(atlas,0,state*h,w,h,copper=state in (2,3),hover=state in (1,3),disabled=state==4)
    atlas.save(OUT/(name+'.png'))
track=Image.new('RGBA',(166,8));panel(track,0,0,166,8);rect(track,(3,3,162,4),NAVY);track.save(OUT/'track.png')
print('Built controller background and six matching control textures')
