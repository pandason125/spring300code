package spring300code;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}
   public static void main(String args[]){
	   String  abc="[tom]";
	   System.out.println(abc.replaceAll("\\[|\\]", "").replaceAll(",\\s", ","));

   }
}
