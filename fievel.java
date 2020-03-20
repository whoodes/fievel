import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.lang.Thread;
import java.lang.Math;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class fievel {

    // https://stackoverflow.com/a/5762502
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static class DepthPair {
        private int depth = -1;
        private ArrayList<String> URLs = new ArrayList<>();

        public DepthPair() {

        }

        /**
         * @param URLs  : A list of URLs found at a certain depth.
         * @param depth : The depth where the URLs were found.
         * @Constructor
         */
        public DepthPair(ArrayList<String> URLs, int depth) {
            this.depth = depth;
            this.URLs = URLs;
        }

        /**
         * @param depth : Relative depth of the pair.
         */
        public void set_depth(int depth) {
            this.depth = depth;
        }

        /**
         * @param URLs : URLs retrieved.
         */
        public void set_URL(ArrayList<String> URLs) {
            this.URLs = URLs;
        }

        /**
         * @return ArrayList
         */
        public ArrayList<String> get_urls() {
            return this.URLs;
        }

        /**
         * @return int
         */
        public int get_depth() {
            return this.depth;
        }

        /**
         * @param url : A single URL to add.
         */
        public void append_url(String url) {
            this.URLs.add(url);
        }

        /**
         * @param urls : An ArrayList of URLs to add.
         */
        public void append_urls(ArrayList<String> urls) {
            this.URLs.addAll(urls);
        }

        /**
         * @param url : Remove a single URL.
         */
        public void remove_url(String url) {
            this.URLs.remove(url);
        }

        /**
         * Remove all URLs from the DepthPair object.
         */
        public void remove_all_urls() {
            this.URLs.clear();
        }

        /**
         * @return String
         * @Override
         */
        public String toString() {
            if (this.URLs.size() == 0) {
                return ANSI_PURPLE + "Nothing to see here!" + ANSI_RESET;
            }

            StringBuilder return_string = new StringBuilder();
            for (String str : this.URLs) {
                return_string.append(str).append("\n");
            }
            return ANSI_PURPLE + "----- DEPTH: " + this.get_depth() + " -----" +
                "\n\n" + ANSI_CYAN + return_string.toString() + ANSI_RESET;
        }
    }

    public static class Crawler {
        private ArrayList<DepthPair> url_pairs = new ArrayList<>();
        private int max_depth;
        private DepthPair root;
        private boolean verbose;
        private int threads;

        /**
         * @param max_depth : Maximum depth to crawl.
         * @param base_url  : The root url to start the crawl.
         * @param threads   : Number of threads to use.
         * @param verbose   : Print non-critical exceptions and meta-data.
         * @Constructor
         */
        public Crawler(int max_depth, String base_url, int threads, boolean verbose) {
            this.max_depth = max_depth;
            ArrayList<String> base = new ArrayList<>();
            base.add(base_url);
            this.root = new DepthPair(base, 0);
            this.threads = threads;
            this.verbose = verbose;
        }

        /**
         * Starts the process.
         */
        public void run_crawler() {
            System.out.println(
                ANSI_GREEN + "Successfully started the web crawler with "
                + threads + " threads!" + ANSI_RESET);
            get_url_pairs(this.root);
        }

        /**
         * This should probably Override toString(), but
         * it doesn't.
         *
         * @return String
         */
        public String print_visited_urls() {
            StringBuilder depth_pairs = new StringBuilder();
            for (DepthPair pair : this.url_pairs) {
                depth_pairs.append(pair.toString()).append("\n");
            }
            return depth_pairs.toString();
        }

        /**
         * The worker responsible for scraping URLs.
         * This nested class does not rely on access to
         * the outer classes attributes.
         */
        public class Worker implements Runnable {
            final private List<String> urls;
            final private ArrayList<DepthPair> global_pairs;
            private boolean verbose;
            private ArrayList<String> visited_urls = new ArrayList<>();
            private DepthPair to_visit = new DepthPair();

            /**
             * @param urls         : URLs for the worker to visit.
             * @param global_pairs : Reference to our list of visited urls at all previous depths.
             * @param verbose      : Passed from the Crawler class, for printing to stdout.
             */
            public Worker(List<String> urls, ArrayList<DepthPair> global_pairs,
                          boolean verbose) {
                this.urls = urls;
                this.global_pairs = global_pairs;
                this.verbose = verbose;
            }

            /**
             * This run method contains the core logic of the web crawler.
             * Each worker that is spawned receives a subset of URLs to visit.
             * <p>
             * First, a socket is created, the contents of the page are parsed for
             * links, we ensure we haven't already visited a given link, and
             * the url is then appended to a list of urls to visit.
             * <p>
             * We also keep track of all the URLs we visit during this process.
             * <p>
             * Each worker has it's own objects for storing the two lists mentioned,
             * this avoids any race conditions when updating lists.  Duplication is
             * handled in the parent, where each worker also returns the data it
             * gathered during a run after all workers have been joined.
             *
             * @Override
             */
            public void run() {
                // We don't care about relative links.
                Pattern url_pattern = Pattern.compile("href=[\"'][^#/\"'](.*?)[\"']");
                for (String url : this.urls) {

                    String path, host;
                    String[] url_value = get_url_components(url);

                    if ((host = url_value[0]).equals("")) {
                        // We had a MalformedURL.
                        continue;
                    }

                    if ((path = url_value[1]).equals("")) {
                        path = "/";
                    }

                    SocketFactory socketFactory = SSLSocketFactory.getDefault();
                    try (Socket socket = socketFactory.createSocket(host, 443)) {
                        socket.setSoTimeout(5000);
                        OutputStream output = socket.getOutputStream();

                        String request = "GET " + path +
                            " HTTP/1.1\r\nConnection: close\r\nHost: " + host + "\r\n\r\n";
                        output.write(request.getBytes(StandardCharsets.US_ASCII));

                        InputStream in = socket.getInputStream();
                        String response = readAsString(in);
                        Matcher url_substrings = url_pattern.matcher(response);

                        while (url_substrings.find()) {
                            String url_to_add = url_substrings.group().split("[\"']")[1];
                            if (get_url_components(url_to_add)[0].equals("")) {
                                continue;
                            }
                            boolean add = true;
                            outer:
                            for (DepthPair pair : this.global_pairs) {
                                // Check for redundant links.
                                for (String u : pair.get_urls()) {
                                    String[] test = get_url_components(url_to_add);
                                    if (u.equals(test[0] + test[1])) {
                                        add = false;
                                        break outer;
                                    }
                                }
                            }
                            if (add) {
                                this.to_visit.append_url(url_to_add);
                            }
                        }
                    } catch (IOException e) {
                        if (this.verbose) {
                            System.out.println(
                                ANSI_YELLOW + "Socket creation failed due to:\n"
                                + "\t" + e.toString() + ANSI_GREEN + "\nSkipping..."
                                + ANSI_RESET);
                        }
                    }
                    this.visited_urls.add(host + path);
                }
            }

            public ArrayList<String> get_urls_to_visit() {
                return this.to_visit.get_urls();
            }

            public ArrayList<String> get_visited_urls() {
                return this.visited_urls;
            }
        }

        /**
         * @param depth_pair : DepthPair Object containing the URLs and depth
         *                   that they were scraped from.
         * @return DepthPair : This updates the Worker.url_pairs member, so
         * a return method just satisfies the recursion.
         */
        private DepthPair get_url_pairs(DepthPair depth_pair) {

            // This function is recursive and uses `max_depth`
            // as the base case.
            if (depth_pair.depth > this.max_depth) {
                return depth_pair;
            }

            if (this.verbose && depth_pair.get_depth() != 0) {
                System.out.println("\n" + ANSI_BLUE + "Crawling at a depth of : "
                    + depth_pair.get_depth() + ANSI_RESET + "\n");
            }

            DepthPair to_visit = new DepthPair();
            to_visit.set_depth(depth_pair.depth + 1);

            ArrayList<String> visited_urls = new ArrayList<>();
            ArrayList<String> visit_list = new ArrayList<>();
            try {
                if (depth_pair.get_urls().size() == 1) {
                    // We always start with one URL.
                    Worker worker = new Worker(
                        depth_pair.get_urls().subList(0, 1),
                        this.url_pairs,
                        this.verbose
                    );
                    Thread thread = new Thread(worker);
                    thread.start();

                    thread.join();
                    visited_urls.addAll(worker.get_visited_urls());
                    visit_list.addAll(worker.get_urls_to_visit());
                } else {
                    Worker[] workers = new Worker[this.threads];
                    Thread[] threads = new Thread[this.threads];
                    double split = Math.floor(
                        depth_pair.get_urls().size() / (double) this.threads);

                    for (int i = 0; i < this.threads; i++) {
                        // A hack for dealing with a bad division.
                        double work_split = split * (i + 1);
                        if (i == this.threads - 1) {
                            work_split = depth_pair.get_urls().size();
                        }
                        workers[i] = new Worker(
                            depth_pair.get_urls().subList(
                                ((int) split * i), (int) work_split),
                            this.url_pairs,
                            this.verbose
                        );
                        threads[i] = new Thread(workers[i]);
                        threads[i].start();
                    }

                    // These `for` loops are important to keep distinct
                    // so that we wait for the work to finish before
                    // trying to aggregate our results.
                    for (int i = 0; i < this.threads; i++) {
                        threads[i].join();
                    }
                    for (int i = 0; i < this.threads; i++) {
                        visited_urls.addAll(workers[i].get_visited_urls());
                        visit_list.addAll(workers[i].get_urls_to_visit());
                    }
                }
            } catch (InterruptedException |
                     ConcurrentModificationException ex) {
                System.out.println(
                    ANSI_RED + "A worker unexpectedly failed!" + ANSI_RESET);
                System.out.println("\t" + ex.toString());
            }

            // Remove all duplicates before going deeper (cue inception music).
            Set<String> set = new HashSet<>(visit_list);
            visit_list.clear();
            visit_list.addAll(set);
            to_visit.append_urls(visit_list);

            // This step doesn't actually guarantee that we won't
            // see duplicates at different depths; Which is somewhat
            // of a problem.
            // TODO: Figure out an efficient solution.
            set.clear();
            set.addAll(visited_urls);
            visited_urls.clear();
            visited_urls.addAll(set);

            DepthPair visited = new DepthPair(visited_urls, depth_pair.get_depth());
            this.url_pairs.add(visited);
            return get_url_pairs(to_visit);
        }

        /**
         * @param inputStream : Input stream from a given socket.
         * @return String      : A string representation of the byte stream.
         * @throws IOException : Handled by the caller.
         */
        private static String readAsString(final InputStream inputStream)
                throws IOException {
            // https://stackoverflow.com/a/45419247
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }

        /**
         * @param url : The URL to have host and path split.
         * @return String[] : The host and path from the passed URL.
         */
        private String[] get_url_components(String url) {
            // This function assumes that caller passes URLs
            // in the from `http[s?]://<host>/[<path>?]`
            URL url_obj;
            try {
                url_obj = new URL(url);
            } catch (MalformedURLException ex) {
                if (this.verbose) {
                    System.out.println(ANSI_YELLOW +
                        "Malformed URL: " + url + "\n\tSkipping..." + ANSI_RESET);
                }
                return new String[]{"", ""};
            }
            return new String[]{url_obj.getHost(), url_obj.getPath()};
        }
    }

    public static void main(String[] args) {
        String arg;
        String url = "https://www.google.com";
        int i = 0;
        int threads = 4;
        int depth = 3;
        boolean verbose = false;

        if (args.length == 0) {
            System.out.println(ANSI_BLUE + "Running in default mode!" + ANSI_RESET);
        }

        // Parse the command line
        // TODO: Handle incorrect arg values.
        while (i < args.length && args[i].startsWith("--")) {
            arg = args[i++];

            switch (arg) {
                case "--url":
                    url = args[i++];
                    break;
                case "--depth":
                    String d = args[i++];
                    depth = Integer.parseInt(d);
                    break;
                case "--verbose":
                    verbose = true;
                    break;
                case "--parallel":
                    if (i < args.length) {
                        String t = args[i++];
                        threads = Integer.parseInt(t);
                    } else {
                        System.out.println(ANSI_RED + "-parallel requires an integer" + ANSI_RESET);
                    }
                    break;
                case "--help":
                    System.out.println(ANSI_BLUE + "\nUsage: " + ANSI_GREEN + "fievel " + ANSI_BLUE +
                        "[--url url] [--depth D] [--verbose] [--parallel N] [--help]\n\n" +
                        "--url:         The website to be used as the root for the\n" +
                        "               recursive web crawl.\n" + ANSI_PURPLE +
                        "                   Default is `https://www.google.com`.\n" + ANSI_BLUE +
                        "--depth:       The cut off depth for the web crawler. The layers\n" +
                        "               of recursion will not exceed this value.\n" + ANSI_PURPLE +
                        "                   Default is 3.\n" + ANSI_BLUE +
                        "--parallel:    The number of threads to be ran during a web crawl.\n" + ANSI_PURPLE +
                        "                   Default is 4.\n" + ANSI_BLUE +
                        "--verbose:     Print all non-critical error messaging, along with\n" +
                        "               meta-data relating to the job.\n" + ANSI_PURPLE +
                        "                   Default is false." + ANSI_RESET);
                    System.exit(0);
            }
        }

        try {
            URL test = new URL(url);
        } catch (MalformedURLException ex) {
            System.out.println(ANSI_RED + "URL should be in the from 'https://www.<hostname>/<path>'" +
                ANSI_RESET);
            System.out.println("Exiting...");
            System.exit(1);
        }

        Crawler crawler = new Crawler(depth, url, threads, verbose);
        crawler.run_crawler();

        System.out.println();
        System.out.println(crawler.print_visited_urls());
    }
}
