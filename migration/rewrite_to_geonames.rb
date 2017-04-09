require 'json'
require './base_rewriter'
require './concordances/wikidata_to_geonames'

class GeoNamesRewriter < BaseRewriter

  def initialize(outfile)
    super(outfile)
    @concordances = WikidataToGeonames.new
  end

  def rewrite_wikidata(body, concordances)
    qid = body["uri"][WIKIDATA_PREFIX.length .. -1]
    match = @concordances.get(qid)
    if (match.nil?)
      puts "No match found"
      body.delete("uri")
      false
    else
      puts "Rewriting #{body["uri"]} -> #{match}"
      body["uri"] = match
      true
    end
  end

  def rewrite_one(annotation)
    rewritten = false

    bodies = annotation["bodies"]
    place_body = getBody(bodies, "PLACE")

    unless place_body.nil?
      uri = place_body["uri"]
      unless uri.nil?
        if uri.start_with?(WIKIDATA_PREFIX)
          rewritten = rewrite_wikidata(place_body, concordances)
        end
      end
    end

    open('out-geonames.jsonl', 'a') { |f| f.puts annotation.to_json }
    rewritten
  end

end

GeoNamesRewriter.new("out-geonames.jsonl").start()
