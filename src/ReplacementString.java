import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * container for the replacement string
 * needed, when we are modifying the boilerplate
 * from a project-agnostic, format-specific
 * object. The key to the Map is the string
 * we look for, and this object contains the
 * string which we will substitute. 
 *
 * ALSO, we allow for substitution by special
 * code. The type flag will be 0 or greater
 * to indicate special processing. Each CreatSpecial
 * child will have its own list of special items,
 * and will have the code to make the special
 * modification.
 *
 * If the flag is -1, no special processing takes
 * place.
 *
 * Updated 4/23/2017
 *
 */
public class ReplacementString
{
    public String rep; // replacement string (for key value)
    public int flag; // -1, nothing special, 0 or greater points to code used in CreateSpecial object
        
    /*
     * simple constructor
     */
    public ReplacementString(String r,
    	int f)
    {
    		rep = r;
    		flag = f;
    }

    /*
     * full toString() override
     */
    public String toString()
    {
    		if (flag < 0)
    		{
	        return "ReplacementString: " + rep + " (simple replacement)";
	     }
    		else
    		{
	        return "ReplacementString: " + rep + " (special replacement, position: " +
	          flag + ")";
    		}	
    } // end tostring full override
    
                  
} // end replacement string container
