require 'csv'
require 'json'
require 'zlib'

WIKIDATA_PREFIX  = "http://www.wikidata.org/wiki/Q"

class WikidataToGeonames

  def initialize()
    @concordances = {}

    puts "Loading Wikidata-to-Geonames concordances"

    gzipped = open("./concordances/data/wikidata_to_geonames.csv.gz")
    gz = Zlib::GzipReader.new(gzipped)

    ctr = 0

    gz.each_line do |line|
      if ctr > 0
        row = line.split(',')

        qid   = row[0]
        gn_id = row[4]

        @concordances[qid] = gn_id
      end

      ctr += 1
    end

    puts "Loaded #{ctr} concordances"
  end

  def get(qid)
    gn_id = @concordances[qid]
    unless gn_id.nil?
      "http://sws.geonames.org/" + gn_id
    end
  end

end
