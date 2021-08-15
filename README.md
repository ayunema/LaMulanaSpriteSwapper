# LaMulanaSpriteSwapper

Customize, mix, and match sprite sheets without wangjangling the contents of your `data/graphics/00` folder
![boop](https://i.imgur.com/JzjFFNV.gif)

## How to do the thing
* The app will try to auto-populate the game directory for you (Feel free to suggest more defaults)
  * If `La-Mulana install directory` is empty, enter the path to your game directory, eg: **C:\Steam\steamapps\common\La-Mulana**
* Select a `Sprite` from the `Sprite` list box
  * Any appropriate variants will populate the `Variant` list box
* Select a `Variant` (from the second-from-the-left list box)
  * ~~You should see an arbirtary image from the set of those that get replaced (This preview area will eventually be used for thumbnails)~~ (Currently no preview exists anymore)
* Check or uncheck `Fresh start` if you want to modify already-modified files
  * This will only revert to default before modification for files that are edited by this particular variant (*[It's complicated](#why-is-fresh-start-complicated)*)
* Check or uncheck `Shuffle colors` if you want to randomize how things look a little
  * If you check `Chaos shuffle` as well, then if the variant has changes across multiple files, it will not attempt to keep them consistent. This causes things to look **really** silly
* Click `Add to queue` (Or just double-click the desired Variant) to add it to the list of modifications
  * If you accidentally added something with the wrong settings checked off, you can modify them after they've been added to the queue, using the matching icon checkboxes on the right
* Click `Apply` to write the changes to your game directory
  * This might take a few seconds if there are many files being touched

The code is ugly, and will be for a while.



## FAQ




### Why is Fresh Start complicated?
  1. Let's say you set Lemeza to Invisible and apply; this modifies like 7 files, and in most cases, only certain parts of those files (`Masks` will be documented eventually)
  2. Now let's say you apply the Moogle variant, which at the moment is only touching 00prof.png
  * If `Fresh start` is **enabled**, then 00prof.png will be reverted and then converted to the Moogle sprite  
  * If `Fresh start` is **disabled**, then any unmasked area in the variant will be preserved  
    * Moogle doesn't edit shields, but Invisible does. So if you use `Fresh start`, the shields will be reverted to the default value. If not, shields will still be invisible

**Why would I want this?**

* If you first apply `Invisible` to Lemeza, then `Sunglasses` ...
 * `Fresh start` = **Enabled** -> Put sunglasses on Lemeza
 * `Fresh start` = **Disabled** -> Put sunglasses floating in the air
 
 **Okay but why didn't you start with that instead of the more-complicated Moogle example?**
 
 I am not good at this.
 
 ## Custom sprite creators
 * Virus610
   * Lemeza/Moogle
   * Lemeza/Invisible
   * Lemeza/Sunglasses-overlay
   * Tiamat/Tee-amat
   * Tiamat/Invisible
   * Tiamat/rainbow
 * Nano
   * Lemeza/Lumisa-Bluemisa
   * Lemeza/Lumisa-KimonoCowgirl



## Known Issues
AKA a lazy TODO list/issue tracker

* ~~App still kinda looks like garbage~~ Honestly, I kinda like how it looks, these days
* Code COULD be less garbagey
* Needs more sprites/variants
* Stuff doesn't necessarily look good on all Operating Systems, yet
