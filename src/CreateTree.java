import java.io.PrintWriter;
import java.io.InputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Properties;


// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
 * updated 2/9/2019
 * 
 * Read the JSON for a query tree and results and
 * make lots of HTML files for the "machine cancel finder"
 * 
 * TODO: 
 * 
 * o) REDESIGN a bit. The TODO item first said: 
 *    
 *    Create all "unlinked" endpoints, that is, those that
 *    remain uncreated after the tree is built. This allows 
 *    endpoint to be created with the same look and feel.
 *    Require this as a command-line option
 * 
 *    INSTEAD! we will create ALL endpoints before any
 *    tree traversal is performed. This allows any number
 *    of "unlinked" endpoints to exist. Other HTML files will
 *    point to them, not just "yes/no" decision queries.
 *    When we reach an endpoint in the tree code, we consider
 *    ourselves to be finished, no more HTML is created.
 * 
 *    One hangup may occur if we make multiple runs, in which
 *    case we overwrite the previous endpoint code.
 * 
 * o) (FIXED I think) FIX bug where we aren't "popping" up the crumb list
 *    The list gets longer and longer instead of showing
 *    only the current path.
 * 
 * o) (DONE) Add metadata for HTML files. This includes keywords and
 *    description. There is boilerplate present, but it must
 *    be replaced by page-specific items to enhance the metadata
 *    already there. This involves adding fields to the JSON
 * 
 * o) (DONE)Require specification of the "top" of the tree. This allows
 *    unconnected decision trees to be created by multiple
 *    executions of this program
 * 
 * o) (DONE) Change HTML file naming to be less wordy. "query1" becomes
 *    "q_1", "endpointBARR_FKYE" becomes "e_BARR_FYKE".
 *
 */
public class CreateTree
{
	public boolean DEBUG_FLAG = false;

	public String top; // top of tree of decisions

	/*
	 * key to map is the name
	 * payload is Query object
	 */
	TreeMap all_queries = new TreeMap();

	/*
	 * key to map is the endpoint name
	 * payload is Endpoint
	 */
	TreeMap all_endpoints = new TreeMap();

	ArrayDeque crumbs = new ArrayDeque(); // for bread crumbs

	List page_end_array = null; // boilerplate

	List static_header_array = null; // boilerplate

	static final String WORK_AREA = "sandbox/"; // working dir

	String date_contents = "";
	String copyright_contents = "";

    /*
     * project-specific strings that are to be replaced
     *
     * key = string to search for in boilerplate
     * 
     * value = ReplacementString object that has the
     *   replacement string, and a flag to indicate special processing
     * 
     * BUILT INSIDE RECURSIVE methods, rather than global, since 
     * each instance needs its own metadata
     * 
     * TreeMap page_keys = null; // populate just before page creation 
     */

    
    public static void main (String[] arg)
    {
        try
        {
            CreateTree t = new CreateTree();
            t.init(arg);
            t.execute();
            t.finish();
        } // end try
        catch (Exception trouble)
        {
            System.err.println("problem: " + trouble);
            trouble.printStackTrace();
        }
    } // end main

    public void init(String[] arg) throws Exception
    {
        if (arg.length < 2)
        {
                throw new Exception("Usage 'input JSON' 'topname'  ");
        }
	top = arg[1]; // top name
        /*
         * read in the JSON data 
         */
        File inputfile = new File(arg[0]);

	ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
	Map<String,Object> userData = mapper.readValue(inputfile,Map.class);
        
        // userData is a Map containing the named arrays
                
	//System.out.println("the map: " + userData);

	page_end_array = (List)userData.get("page_end"); // List of strings with boilerplate for HTML page end
	static_header_array = (List)userData.get("static_header"); // List of strings with boilerplate for start of each HTML page
	List query_array = (List)userData.get("queries"); // List of Lists containing Query object contents

	List endpoint_array = (List)userData.get("endpoints"); // List of Lists containing Endpoint object contents

	List date_stuff = (List)userData.get("date"); // a single string with data date
	date_contents = (String)date_stuff.get(0); // a single string with data date
	List copyright_stuff = (List)userData.get("copyright"); // a single string with copyright HTML
	copyright_contents = (String)copyright_stuff.get(0); // a single string with copyright HTML

	/*
	 * read queries and build a Tree of them for later transversal
	 */
	Iterator ii = query_array.iterator();
	// loop on each query entry
	Query h = null;

	while (ii.hasNext())
	{
		List single_query = (List)ii.next(); // load it up
		/*
		 * make into Query object
		 */
		h = new Query(single_query); // makes from String and/or List

		/*
		 * MUST BE UNIQUE, check it
		 */
		if (all_queries.containsKey(h.name))
		{
			throw new Exception("Duplicate Query: " +
				h);
		}
		all_queries.put(h.name,h); // key = name; payload = Query

	} // end for each query

	/*
	 * read endpoints and build a Tree of them for later usage
	 */
	ii = endpoint_array.iterator();
	// loop on each endpoint entry
	Endpoint e = null;

	while (ii.hasNext())
	{
		List single_endpoint = (List)ii.next(); // load it up
		/*
		 * make into Endpoint object
		 */
		e = new Endpoint(single_endpoint); // makes from String and/or List

		/*
		 * MUST BE UNIQUE, check it
		 */
		if (all_endpoints.containsKey(e.name))
		{
			throw new Exception("Duplicate Endpoint: " +
				e);
		}
		all_endpoints.put(e.name,e); // key = name; payload = endpoint

	} // end for each endpoint

	/*
	 * NEW CODE, we will create ALL endpoints now. They will NOT
	 * be created when we traverse the decision tree.
	 * OVERWRITE issue?
	 * BIGGER issue: we want the breadcrumbs on each page, BUT
	 * that is dependent on where it was called. In the case
	 * of multiple references to a single endpoint page,
	 * it won't work. So, we put in narrative instead....
	 */
	ii = all_endpoints.keySet().iterator();
	while (ii.hasNext())
	{
		String nam = (String)ii.next();
		Endpoint endx = (Endpoint)all_endpoints.get(nam);
		/*
		 * set up replacement strings for boilerplate embedded in page
		 */
		TreeMap page_keys = new TreeMap(); // new for every page
		page_keys.put("PAGE_COPYRIGHT",
			new ReplacementString(copyright_contents,-1)); // simple
		page_keys.put("PAGE_PUBLISH_DATE",
			new ReplacementString(date_contents,-1)); // simple
		page_keys.put("PAGE_TITLE",
			new ReplacementString(endx.title,-1)); // simple
		page_keys.put("ADDITIONAL_PAGE_DESCRIPTION",
			new ReplacementString(endx.additional_description,-1)); // simple
		page_keys.put("ADDITIONAL_KEYWORDS",
			new ReplacementString(endx.additional_keywords,-1)); // simple
		// FOLLOWING OK? we will probably overwrite previous versions in output area
		PrintWriter pr = new PrintWriter(WORK_AREA + 
			createHTML(nam) +
			 ".html"); // endpoint name. html
		dump_strings(pr,static_header_array,page_keys);
		dump_strings(pr,endx.html,page_keys); // all endpoint descriptors and images
	System.out.println("endpoint: " + endx);
		/*
		 *  NOTE breadcrumbs get put here
		Iterator crumb_iterator = crumbs_content.iterator();
		pr.println("<div style=\"clear:left;\"><p>Previous Queries: ");
		while (crumb_iterator.hasNext())
		{
			String prev = (String)crumb_iterator.next();
			pr.println("<a href=\"" + 
				createHTML(prev) + ".html\">" +
				prev + "</a> -- ");
		}
		pr.println("</p></div>");
		*/
		// write narrative!
		pr.println("<div style=\"clear:left;\"><p>Use the browser <b>BACK</b> button to return to the page that invoked this page!</p></div>");
		dump_strings(pr,page_end_array,page_keys);
		pr.close();
	} // end loop on all endpoints (by name) and create pages NOW
        
    } // end init

 public void execute() throws Exception
    {
		/*
		 * traverse the tree and 
		 * make webpages 
		 * 
		 * start at first 
		 */
		crumbs.push(top); // name for now
		Query first = (Query)all_queries.get(top);
		/*
		 * set up replacement strings for boilerplate embedded in page
		 */
		TreeMap page_keys = new TreeMap(); // new for every page
		page_keys.put("PAGE_COPYRIGHT",
			new ReplacementString(copyright_contents,-1)); // simple
		page_keys.put("PAGE_PUBLISH_DATE",
			new ReplacementString(date_contents,-1)); // simple
		page_keys.put("PAGE_TITLE",
			new ReplacementString(first.title,-1)); // simple
		page_keys.put("ADDITIONAL_PAGE_DESCRIPTION",
			new ReplacementString(first.additional_description,-1)); // simple
		page_keys.put("ADDITIONAL_KEYWORDS",
			new ReplacementString(first.additional_keywords,-1)); // simple
		
		PrintWriter pr = new PrintWriter(WORK_AREA + 
			createHTML(top + ".html"));
		dump_strings(pr,static_header_array,page_keys);
		dump_strings(pr,first.html,page_keys); // all query descriptors and images
		/* 
		 * get destination(s) and display them, if Query
		 * if Endpoint, nothing will happen
		 */ 
	System.out.println("first: " + first);
		print_destination(pr, first.yes); // Yes
		print_destination(pr, first.no); // No
		// NOTE no breadcrumbs with the first query
		dump_strings(pr,page_end_array,page_keys);
		pr.close();
	/*
	 * NOW, we will process the "yes no" destinations
	 * for "query". NOTE it is recursive.
	 */
		process_query(first.yes.jump,crumbs); // Yes
		process_query(first.no.jump,crumbs); // No
     } // end execute 

 public void finish() throws Exception
    {
	Iterator ii = null;

	/*
	 * HERE HERE at the end, we will create ALL unlinked
	 * endpoints. This can be driven by a command-line
	 * option. For now, just list them.
	System.out.println("\nAll UNLINKED Endpoints By Name\n\n");
	ii = all_endpoints.keySet().iterator();
	while (ii.hasNext())
	{
		String nam = (String)ii.next();
		Endpoint oo = (Endpoint)all_endpoints.get(nam);
		if (!oo.created)
		{
			System.out.println(oo);
		}
	} // end loop on all endpoints (by name)
	 */
	if (DEBUG_FLAG)
	{
		/*
		 * list queries by name
		 */
		System.out.println("\nAll Queries By Name\n\n");
		ii = all_queries.keySet().iterator();
		while (ii.hasNext())
		{
			String nam = (String)ii.next();
			// key is name
			Query oo = (Query)all_queries.get(nam);
			System.out.println(oo);
		} // end loop on all queries (by name)
		/*
		 * list endpoints by name
		 */
		System.out.println("\nAll Endpoints By Name\n\n");
		ii = all_endpoints.keySet().iterator();
		while (ii.hasNext())
		{
			String nam = (String)ii.next();
			Endpoint oo = (Endpoint)all_endpoints.get(nam);
			// key is payload
			System.out.println(oo);
		} // end loop on all endpoints (by name)
	} // end debug

     } // end finish 

	/*
	 * create an actual HTML filename, used to both
	 * write the file and to create links TO the file
	 * 
	 * rather than use the naming we have for 
	 * queries and endpoints, we will instead create
	 * short HTML filenames
	 */
	public String createHTML(String name)
	{
		if (name.startsWith("query"))
		{
			return "q_" + name.substring(5);  // becomes q_1, etc
		}
		// endpoint
		return "e_" + name.substring(8);  // becomes e_SOMETHING
	}

	/*
	 * this processes a destination
	 * 
	 * for all types, we create the HTML file 
	 * 
	 * if a "query", we recurse on the "yes" and "no" 
	 * 
	 * if an "endpoint", we simply leave , the files having ALREADY
	 * been created!
	 */
	public void process_query(String name,
		ArrayDeque crumbs_content) throws Exception
	{
		if (name.startsWith("query"))
		{
			Query current = (Query)all_queries.get(name);
			if (current == null)
			{
				System.out.println("NOT FOUND: " + name);
				return; // unwind, we can't go farther
			}
// debug	System.out.println("current: " + current);
			/*
			 * set up replacement strings for boilerplate embedded in page
			 */
			TreeMap page_keys = new TreeMap(); // new for every page
			page_keys.put("PAGE_COPYRIGHT",
				new ReplacementString(copyright_contents,-1)); // simple
			page_keys.put("PAGE_PUBLISH_DATE",
				new ReplacementString(date_contents,-1)); // simple
			page_keys.put("PAGE_TITLE",
				new ReplacementString(current.title,-1)); // simple
			page_keys.put("ADDITIONAL_PAGE_DESCRIPTION",
				new ReplacementString(current.additional_description,-1)); // simple
			page_keys.put("ADDITIONAL_KEYWORDS",
				new ReplacementString(current.additional_keywords,-1)); // simple
		
			PrintWriter pr = new PrintWriter(WORK_AREA + 
				createHTML(name) +
				 ".html"); // query name. html
			dump_strings(pr,static_header_array,page_keys);
			dump_strings(pr,current.html,page_keys); // all query descriptors and images
			/* 
			 * get destination(s) and display them, if Query
			 * if Endpoint, nothing will happen
			 */ 
			print_destination(pr, current.yes); // Yes
			print_destination(pr, current.no); // No
			// NOTE breadcrumbs get put here
			Iterator crumb_iterator = crumbs_content.iterator();
			pr.println("<div style=\"clear:left;\"><p>Previous Queries: ");
			while (crumb_iterator.hasNext())
			{
				String prev = (String)crumb_iterator.next();
				pr.println("<a href=\"" + 
					createHTML(prev) + ".html\">" +
					prev + "</a> -- ");
			}
			pr.println("</p></div>");
			dump_strings(pr,page_end_array,page_keys);
			pr.close();
			// we recurse for each of the yes/no choices
			crumbs_content.push(name); // HERE HERE must pop eventually
			process_query(current.yes.jump,crumbs_content); // Yes
			process_query(current.no.jump,crumbs_content); // No
			crumbs_content.pop(); // no need to read
		} // end query processing
		else
		{
			// Endpoint
			Endpoint endx = (Endpoint)all_endpoints.get(name);
			if (endx == null)
			{
				System.out.println("NOT FOUND: " + name);
				return; // nothing done 
			}
// NEVER pop an endpoint, we didn't push it in			crumbs_content.pop(); // no need to read
		} // end endpoint processing, fall through
	} // end process_query

	public void print_destination(PrintWriter pr, Destination dd)
	{
		if (dd.jump.startsWith("query")) 
		{
			pr.println("<div style=\"clear:left;\">");
			Query yes = (Query)all_queries.get(dd.jump);
			if (yes != null)
			{
				pr.println("<p>" + dd.getHTML() +
					"</p>\n");
			}
			else
			{
				// NOT FOUND, use dummy
				pr.println("<p><a href=\"dummy.html\">" +
					dd.wording +
					" (DUMMY: " + dd.jump +")</a></p>\n");
				System.out.println("BAD DESTINATION: " +
					dd.jump + ".html");
			} // end didn't find the query destination
			pr.println("</div>");
		} // end query destination
		else
		{
			// endpoint display
			pr.println("<div style=\"clear:left;\">");
			Endpoint yese = (Endpoint)all_endpoints.get(dd.jump);
			if (yese != null)
			{
				pr.println("<p>" + dd.getHTML() +
					"</p>\n");
			}
			else
			{
				// NOT FOUND, use dummy
				pr.println("<p><a href=\"dummy.html\">" +
					dd.wording +
					" (DUMMY: " + dd.jump + ")</a></p>\n");
				System.out.println("BAD DESTINATION: " +
					dd.jump + ".html");
			} // end didn't find the endpoint destination
			pr.println("</div>");
		}
	} // end process destination

	public void dump_strings(PrintWriter pr, List content,
		TreeMap page_keys) throws Exception
	{
		Iterator ii = content.iterator(); // assume strings
		while (ii.hasNext())
		{
			String c = (String)ii.next();
			/*
			 * we can alter the string, based
			 * on the presence of a special marker
			 * Examples include additional metadata
			 */
			pr.println(singleStringReplace(c, page_keys)); // newline forced
		} // loop through all strings in List
	} // end dump strings

	/*
	 * given a single String, perform the replacement
	 * that we normally do with stringReplacer. It
	 * is designed to walk only String values inside
	 * a JSON array. This one can be called
	 * by anybody who has a String to be modified
	 */
	public String singleStringReplace(String test,
	Map project_keys)
	{
		String result = test; // working copy
		String akey = "";
		//boolean did_something = false; // keep flag, could be many replacements in a line
		Iterator inner = project_keys.keySet().iterator(); // all search keys
		while (inner.hasNext())
		{
			akey = (String)inner.next();
			if (result.indexOf(akey ) >= 0)
			{
				// HIT IT!
			//	did_something = true;
				ReplacementString rval = (ReplacementString)project_keys.get(akey);
				// replace back over for future testing
				result = replaceAString(test,akey,rval); // get replacement, whether normal or special							
			} // end if found one of the keys
		} // end check all keys (may be more than one in a line)
		return result; // either copy of original string, or modified version		
	} // end single string replace

	/*
	 * test = full string within which we will perform replacement
	 * akey = string to be replaced
	 * rval = ReplacementString object, which contains the string
	 *    that will replace "akey", and a flag that indicates
	 *     whether special processing will occur.
	 */
	public String replaceAString(String test, String akey, ReplacementString rval)
	{
			if (rval.flag < 0)
			{
				// NORMAL replacement
				String result = test.replace(akey ,rval.rep); // simple replace
				// debug System.out.println("Replaced: " +
					// 				result);
			// caller decides to do this	ii.set(result); // overwrites current boilerplate string
			  return result; // modified string
			} // end normal string replacement
			else
			{
				// SPECIAL PROCESSING, not simple string replacement
					 			
				String res2 = specialReplacementProcessing(
					 			  rval);  // replace with returned string
			// caller does this	ii.set(res2);
				// debugSystem.out.println("Special Replaced: " +
				//	 				res2);
				return res2; // modified string
			} // end special processing
	} // end replaceastring
	/*
	 * 
	 * return a String that will be written to HTML
	 */
	public String specialReplacementProcessing(ReplacementString rval)
	{
		StringBuffer result = new StringBuffer();
		switch (rval.flag)
		{
			case 0:
			{
			    	// "PROJECT_FRONT_MATTER"
				result.append("<h1>"); // front is a heading
			    	String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append(xx[inner] +
        					"<br/>" + "<br/>");
        				if (inner == 0)   // first line only
        				{
        					result.append("</h1>" +
						"<p style=\"text-align: center;\">" +
						"<br/>");

        				} // end first line only
				}  // end loop through all strings to be treated as separate lines         
				result.append("</p>");
			    	break;
			} // end 0 which is PROJECT_FRONT_MATTER
			case 1:
			{
			    	// "PROJECT_KEYWORDS"
			    	String xx[] = rval.rep.split(":"); // 1 (one) colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append(xx[inner] +
        					","); // keywords are comma-delimited
				}  // end loop through all strings to be treated as separate lines         
				result.append("web"); // dummy for end
			    	break;
			} // end 1 which is PROJECT_KEYWORDS
			case 2:
			{
			    	// "PROJECT_COPYRIGHT"
				result.append( "<meta name=\"copyright\" content=\"" + 
			    	rval.rep + "\"/>");
			    	break;
			} // end 2 which is PROJECT_COPYRIGHT
		} // end switch on special code segments
		return result.toString();		
	}	// end specialReplacementProcessing

	
/* OBJECTS */

    private class Query implements Comparable
    {
	/*
	 * container for Query object
	 * for sorting and lookup, the KEY is
	 * the query name, which MUST BE UNIQUE
	 *
	 */

	String name; // query name
	String title; // title for web page
	String additional_description; // added to meta description on HTML page
	String additional_keywords; // added to meta keywords for HTML page (a comma is ALREADY present)
	List html; // HTML to be inserted into the web page
	Destination yes;
	Destination no;

	/*
	 * build object from List of strings
	 * 
	 * from documentation: 
	 * 
	"each query item is an array of strings ",
	"1 - name of query (ALWAYS starts with the word 'query')",
	   "the query tree always starts with specified value (maybe 'query1')",
	"2 - title for HTML page",
	"3 - additional description for the web page (added to boilerplate, which see)",
	"4 - additional keywords for the web page (added to boilerplate, which see, it already has a comma)",
	"5 - question HTML code, array of strings",
	"6 - YES prompt and destination, array of strings:",
	"    1 - YES wording",
	"    2 - name of destination (MUST start with 'query' or 'endpoint')",
	"7 - NO prompt and destination, array of strings:",
	"    1 - NO wording",
	"    2 - name of destination (MUST start with 'query' or 'endpoint')"
	 */
        public Query(List xx)
        {
		name = (String)xx.get(0); // must work (name)
		title = (String)xx.get(1); // must work (title)
		additional_description = (String)xx.get(2); // must work (desc)
		additional_keywords = (String)xx.get(3); // must work (key)
		html = (List)xx.get(4); // must work (question strings for HTML)
		List yesl = (List)xx.get(5); // must work (List with response)
		yes = new Destination((String)yesl.get(0), // query or endpoint 
			(String)yesl.get(1)); // HTML for YES answer
		List nol = (List)xx.get(6); // must work  (List with response)
		no = new Destination((String)nol.get(0), // query or endpoint 
			(String)nol.get(1)); // HTML for YES answer
        }
        
        public boolean equals(Object xx)
        {
            if (xx instanceof Query)
            {
                Query ff = (Query)xx;

		return ff.name.equals(this.name);
            } // end if matching object types
            else
            {
                return false; // never the same
            }
        } // end equals
        
        public int compareTo(Object xx)
        {
            if (xx instanceof Query)
            {
                Query ff = (Query)xx;
		return ff.name.compareTo(this.name);
            } // end if matching object types
            else
            {
                return -1; // never the same
            }
        } // end compareto

	public String toString()
	{
		StringBuffer bb = new StringBuffer();
		bb.append("\nQuery " + name + "\n");
		bb.append("   " + html + "\n");
		bb.append("YES:  " + yes.jump + "\n");
		bb.append(" NO:  " + no.jump + "\n");
		return bb.toString();
	} // end toString
    } // end Query

    private class Endpoint implements Comparable
    {
	/*
	 * container for Endpoint object
	 * for sorting and lookup, the KEY is
	 * the endpoint name, which MUST BE UNIQUE
	 *
	 */

	String name; // endpoint name
	String title; // title for HTML page
	String additional_description; // added to meta description on HTML page
	String additional_keywords; // added to meta keywords for HTML page (a comma is ALREADY present)
	List html; // HTML to be inserted into the web page
	//boolean created; // indicates whether the HTML file has been created

	/*
	 * build object from List of strings
	 * 
	 * from documentation: 
	 * 
	"the layout of the endpoints is as follows:",
	"",
	"endpoints contains arrays of endpoint items ",
	"",
	"each endpoint item is an array of strings ",
	"1 - name of endpoint (ALWAYS starts with the word 'endpoint')",
	"2 - title of HTML web page"
	"3 - additional description for web page (in addition to boilerplate, which see)",
	"4 - Additional keywords for web page (in addition to boilerplate, which see, note comma already present)",
	"5 - final HTML code, array of strings"
	 */
        public Endpoint(List xx)
        {
		name = (String)xx.get(0); // must work (name)
		title = (String)xx.get(1); // must work (title)
		additional_description = (String)xx.get(2); // must work (desc)
		additional_keywords = (String)xx.get(3); // must work (key)
		html = (List)xx.get(4); // must work (strings for HTML)
		//created = false;
        }
        
        public boolean equals(Object xx)
        {
            if (xx instanceof Endpoint)
            {
                Endpoint ff = (Endpoint)xx;

		return ff.name.equals(this.name);
            } // end if matching object types
            else
            {
                return false; // never the same
            }
        } // end equals
        
        public int compareTo(Object xx)
        {
            if (xx instanceof Endpoint)
            {
                Endpoint ff = (Endpoint)xx;
		return ff.name.compareTo(this.name);
            } // end if matching object types
            else
            {
                return -1; // never the same
            }
        } // end compareto

	public String toString()
	{
		StringBuffer bb = new StringBuffer();
		bb.append("\nEndpoint " + name + "\n");
		bb.append("   " + html + "\n");
		return bb.toString();
	} // end toString
    } // end Endpoint

	/*
	 * container for destination, includes wording and jump
	 */
	public class Destination
	{
		String wording;
		String jump;

		public Destination(String w, String j)
		{
			wording = w;
			jump = j;
		}

		public String toString()
		{
			return "Jump to: " + createHTML(jump) +
				".html, when wording: " +
				wording + " clicked.";
		}

		public String getHTML()
		{
			return "<a href=\"" + 
				createHTML(jump) + ".html\">" +
				wording + "</a>";
		}
	} //end destination
} // end create tree
