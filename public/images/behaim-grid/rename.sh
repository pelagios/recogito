for f in TileGroup0/*.png; do 
mv -- "$f" "${f%.png}.jpg"
done
