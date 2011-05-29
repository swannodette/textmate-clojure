# textmate-clojure

A TextMate bundle for Clojure. Check out the screencast [here](http://blip.tv/file/4160578).

## Installation

This bundle has only been tested with Clojure 1.2.0, though we'd love to hear experience reports about 1.1.0.

    $ cd ~/Library/Application\ Support/TextMate/Bundles
    $ git clone git://github.com/swannodette/textmate-clojure.git Clojure.tmbundle
    $ osascript -e 'tell app "TextMate" to reload bundles'

## RVM Issues

If you are using RVM it is important to follow the TextMate integration instructions pretty closely. More details of the issues on this [thread](http://groups.google.com/group/textmate-clojure/browse_thread/thread/7d42a13f50c98fdd)

## Cake Integration

This fork adds quite a few commands via [Cake](http://github.com/ninjudd/cake) which makes the TextMate experience considerably more "Lispy".

First you need to install Cake.

    sudo gem install cake
    
If you already have Cake installed make sure you have at least version 0.5.6.

## Tutorial

### First steps

Once installed you can create and save a new file with the <code>.clj</code> extension. Type the following expression into this new file and place your cursor as indicated:

<pre>
(+ 4 5)
-------^
</pre>

Type <code>Control-X</code>. If you haven't saved the file yet, you will be prompted to do so. The first time you run this command it will take several seconds to see a result. This is because the JVM is starting up. After the first time, <code>Control-X</code> will be much, much, much faster.

### Projects

Most of the time you won't be working with single files. You'll be working with projects. Let's make a new project:

<pre class="console">
cake new hello-world
cd hello-world
mate .
</pre>

Change your <code>project.clj</code> to look like the following:

<pre>
(defproject hello-world "0.0.1-SNAPSHOT"
  :description "TODO: add summary of your project"
  :dependencies [[org.clojure/clojure "1.2.0"]])
</pre>

Then from the command line:

<pre class="console">
cake deps
</pre>

Create a source file in your <code>src</code> directory called <code>hello_world.clj</code>. Paste this code into there:

<pre>
(ns hello-world)

(defn hello-world []
  (println "Hello world!"))
</pre>

Type <code>Command-Shift-L</code>. This will load your file. You now have a function that you can run. One way is by typing the following and <code>Control-X</code> in the position indicated:

<pre>
(hello-world)
-------------^
</pre>

But this would be ignoring the versatility of Cake's REPL. At the command line from your project directory type:

<pre class="console">
cake repl
</pre>

This will drop you into the same persistent REPL that your project is using, type the following incomplete sexpr and press the <code>Tab</code> key. This should autocomplete the hello-world namespace:

<pre>
user=> (in-ns 'he
</pre>

Close the paren and press enter. You are now in the namespace of the file you are currently working on. Running the <code>hello-world</code> fn from here is an exercise left to the reader ;)

### Existing Projects

This bundle works great with your [lein](http://github.com/technomancy/leiningen) projects. Just <code>cd</code> into them and run <code>mate</code>. TextMate will load the directory you can just type <code>Command-R</code> and this will start Cake.

### Available Commands

* Cake Start : Start up a persistent REPL
* Cake Restart : Restart Cake
* Load File : load the entire file into the current REPL.
* Eval : will evaluate the selected code in the current REPL for the project directory.
* Eval Last Sexpr : will evaluate the sexpr immediate before the cursor
* Wrap Sexpr : wrap the selected sexpr
* Unwrap Sexpr : unwrap the selected sexpr
* Autocomplete : autocomplete a partially typed symbol
* Show Source : shows the source of the selected function in a new window.
* Show Doc : shows the document of the selected function in a new window.
* Jump To Definition : jump to the definition of a symbol (currently only works on files not in jars)
* Macroexpand : macroexpands the selected sexpr.
* Macroexpand all : fully macroexpands the selected sexpr.

## Hacking & Contributing

This bundle is written almost entirely in Clojure. All commands trigger Clojure scripts which you can find in the bundle under <code>textmate-clojure/Support/bin</code>. Feel free to fork and contribute. There's also a support mailing list [here](http://groups.google.com/group/textmate-clojure).

## Tips

In order to get proper word movement in Clojure you might want to set your Word Characters to <code>_/-.:</code> in the Text Editing tab of the TextMate Preferences window.

For an even more SLIME like experience you could install [Visor](http://visor.binaryage.com/) so that switching to the REPL is just a key-stroke away.

## Acknowledgements

This bundle is based on the Based on a previous Clojure bundle by [stephenroller](http://github.com/stephenroller/clojure-tmbundle) and [mmcgrana](http://github.com/mmcgrana/textmate-clojure).