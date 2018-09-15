public class RetrieveStats {
 
    static String host = "http://localhost:9000/sonar";
    static String resourceKey = "com.apple.wwrc.service.customer:wwrc-customer:wwrc-customerthroughlocalmvn";
    static String[] MEASURES_TO_GET = new String[]{"violations", "new_violations", 
                                                   "new_coverage", "tests"};
 
    public static void main(String[] args) {                
        try {       
            //setup
            DecimalFormat df = new DecimalFormat("#.##");            
            Date date = new Date();
 
            //header
            output("************************************");
            output("Code trend for the past 7 days as of "+date);
            output("************************************");
 
            //do the work of getting the data
            Sonar sonar = new Sonar(new HttpClient4Connector(new Host(host)));
            ResourceQuery query = ResourceQuery.createForMetrics(resourceKey, MEASURES_TO_GET);
            query.setIncludeTrends(true);
            Resource resource = sonar.find(query);
            //loop through them
            //getVariation2 for "7 days"
            List<measure> allMeasures = resource.getMeasures();
            for (Measure measure : allMeasures) {
                output(measure.getMetricKey()+": "+df.format(measure.getVariation2()));
            }            
            output("DONE");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
</measure>