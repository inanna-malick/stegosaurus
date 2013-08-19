stegosaurus
===========

stegosaurus is a single-page web-app that simplifies the process of steganographically 
hiding messages inside images and sharing those images via Imgur and Twitter. Users provide a message, 
an image and a key, which are combined to produce a subtly altered image. 
This image can be downloaded directly or uploaded to Imgur and shared via Twitter.
Users can also retrieve messages from images, assuming they possess the correct key.

This [implementation of the F5 algorithm](https://code.google.com/p/f5-steganography/) performs the crypto magic. 

>Abstract:
>Many steganographic systems are weak against visual and statistical attacks. 
>Systems without these weaknesses offer only a relatively small capacity for steganographic messages. 
>The newly developed algorithm F5 withstands visual and statistical attacks, 
>yet it still offers a large steganographic capacity. F5 implements matrix encoding to improve the 
>efficiency of embedding. Thus it reduces the number of nec- essary changes. 
>F5 employs permutative straddling to uniformly spread out the changes over the whole steganogram.
>[Andreas Westfeld, F5 Steganography](http://f5-steganography.googlecode.com/files/F5%20Steganography.pdf)

TODO
+ enforce HTTPS. Seriously, I'm sending keys and messages encoded in URLS.  This app is esentially a toy, don't use it to blow any whistles
+ enable pulling images directly from URLS, for both message embeding and extraction
