package ng.com.obkm.exquisitor.database;

//command to generate 1000 columns in scala
// scala> for(w <- 0 to 1000){println("public static final String PROB" + i +" = " + """"prob"""+i+'"'+";"); i=i+1;}

public class VectorDBSchema {

    public static final class VectorTable {
        public static final String NAME = "Vectors";

        public static final class Cols {
            public static final String IMAGEID = "imageID";
            public static final String SEEN = "seen";
            public static final String FEATURES = "features";
            public static final String PROBS = "probs";
        }
    }
}
