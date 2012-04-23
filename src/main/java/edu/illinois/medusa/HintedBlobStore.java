package edu.illinois.medusa;

import javax.transaction.Transaction;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Blob Store that can use Akubra hints to maintain Caringo headers
 *
 * @author Howard Ding - hding2@illinois.edu
 */

public class HintedBlobStore extends CaringoBlobStore {

    /**
     * Hints to be used over the whole store
     */
    protected CaringoHints hints;

    protected HintedBlobStore(URI storeId, String configFilePath) {
        super(storeId, configFilePath);
    }

    /**
     * Open a connection to Caringo storage
     *
     * @param tx    For this BlobStore this must be null - transactions are unsupported.
     * @param hints Any hints to initialize this connection
     * @return new connection
     * @throws IOException If there is any problem opening the connection or tx is not null.
     */
    public HintedBlobStoreConnection openConnection(Transaction tx, Map<String, String> hints) throws IOException {
        if (tx != null) {
            throw new UnsupportedOperationException();
        }
        return new HintedBlobStoreConnection(this, streamManager, this.hints.copy_and_merge_hints(hints));
    }

    /**
     * Open a connection to Caringo storage
     *
     * @return new connection
     * @throws IOException If there is any problem opening the connection
     */
    public HintedBlobStoreConnection openConnection() throws IOException {
        return this.openConnection(null, null);
    }

    //currently this doesn't do anything, but I'm putting it here in preparation for being able to convert
    //header.x = y|z properties into headers.
    //This would involve:
    //Extract those keys (header.<header-name>)
    //Parse keys and values
    //Add to hints
    protected void configFromProperties(Properties config) {
        this.hints = new CaringoHints();
        super.configFromProperties(config);
        this.addConfigHeaders(config);
    }

    protected void addConfigHeaders(Properties config) {
        Set<String> keys = config.stringPropertyNames();
        for (String key : keys) {
            if (key.startsWith("header.")) {
                String headerName = key.substring("header.".length());
                List<String> values = parseConfigHeaders(config.getProperty(key));
                for (String value : values) {
                    this.hints.addHint(":" + headerName, value);
                }
            }
        }
    }

    //configString will be an arbitrary string
    //It should be parsed by separating it info fields on the | character. The pipe character may be doubled
    //to quote a pipe character in a value.
    //The return is an array that contain a list of field values.
    protected ArrayList<String> parseConfigHeaders(String configString) {
        ArrayList<String> values = new ArrayList<String>();
        String accumulator = new String();
        StringTokenizer tokenizer = new StringTokenizer(configString, "|", true);
        boolean onPipe = false;
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("|")) {
                if (onPipe) {
                    accumulator = accumulator + "|";
                    onPipe = false;
                } else {
                    onPipe = true;
                }
            } else {
                if (onPipe) {
                    values.add(accumulator);
                    onPipe = false;
                    accumulator = token;
                } else {
                    accumulator += token;
                }
            }
        }
        if (accumulator.length() > 0)
            values.add(accumulator);
        return values;
    }
}

