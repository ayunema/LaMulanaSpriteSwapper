# LaMulanaSpriteSwapper

Customize, mix, and match sprite sheets without wangjangling the contents of your `data/graphics/00` folder
![boop](https://i.imgur.com/YroRL0K.gif)

## How to do the thing
* The app will try to auto-populate the game directory for you (Feel free to suggest more defaults)
  * If `La-Mulana install directory` is empty, enter the path to your game directory, eg: **C:\Steam\steamapps\common\La-Mulana**
      *  TODO: App to remember previously entered location
* Select a `Sprite` from the `Sprite` list box
  * Any appropriate variants will populate the `Variant` list box
* Select a `Variant` (from the second-from-the-left list box)
  * You should see an arbirtary image from the set of those that get replaced (This preview area will eventually be used for thumbnails)
* Check or uncheck `Fresh start` if you want to modify already-modified files
  * This will only revert to default before modification for files that are edited by this particular variant (*[It's complicated](#why-is-fresh-start-complicated)*)
* Click `Apply` to write the changes to your game directory
  * Might take a couple seconds if there are several files being touched

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
