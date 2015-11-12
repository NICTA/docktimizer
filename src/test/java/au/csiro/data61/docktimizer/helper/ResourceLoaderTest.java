package au.csiro.data61.docktimizer.helper;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;

/**
 */
public class ResourceLoaderTest {
    @Test
    public void testLoadingProperties() {
        FileLoader script = new FileLoader();
        assertThat(script.CLOUD_INIT, is(notNullValue()));
    }

    @Test
    public void testRegEx() {
        String name = "k1v1-a75";

        Matcher matcher = Pattern.compile("k[0-9]*v[0-9]*").matcher(name);
        boolean b = matcher.find();

        assertTrue(b);

        String s = StringUtils.substringBetween(name, "k", "v");
        Integer position = Integer.parseInt(s);

        assertThat(position, is(1));
        name = "k13v12-a75";
        s = StringUtils.substringBetween(name, "k", "v");
        position = Integer.parseInt(s);

        assertThat(position, is(13));
    }

}
