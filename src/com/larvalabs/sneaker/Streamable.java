package com.larvalabs.sneaker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author John Watkinson
 */
public interface Streamable {

    void read(DataInputStream in) throws IOException;

    void write(DataOutputStream out) throws IOException;

}
