# fievel
## A multi-threaded web crawler!

Using fievel is easy!
* `javac fievel.java`
* `java fievel`

That will run things in default mode, passing the `--help` command will
show you the following (but in cool colors!):

```
Usage: fievel [--url url] [--depth D] [--verbose] [--parallel N] [--help]

--url:         The website to be used as the root for the
               recursive web crawl.
                   Default is `https://www.google.com`.
--depth:       The cut off depth for the web crawler. The layers
               of recursion will not exceed this value.
                   Default is 3.
--parallel:    The number of threads to be ran during a web crawl.
                   Default is 4.
--verbose:     Print all non-critical error messaging, along with
               meta-data relating to the job.
                   Default is false.
```

---

Have an idea to make fievel better? Submit a pull request! :)
