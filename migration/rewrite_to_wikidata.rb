require 'json'
require './base_rewriter'
require './concordances/maphist_to_wikidata'

MAP_HIST_PREFIX  = "http://www.maphistory.info/portolans/record/"
PASTPLACE_PREFIX = "http://data.pastplace.org/search?q="
WIKIDATA_PREFIX  = "http://www.wikidata.org/wiki/Q"

class WikidataRewriter < BaseRewriter

  def initialize(outfile)
    super(outfile)
    @concordances = WikidataToGeonames.new
  end

  private def parseURI(uri)
    id = uri[MAP_HIST_PREFIX.length .. -1].split('-')
    { "sorting" => id[1], "line_number" => id[0] }
  end

  # Type needs to be change from QUOTE to TRANSCRIPTION
  def rewrite_quote(quote_body)
    quote_body["type"] = "TRANSCRIPTION"
  end

  # Just a simple string replace
  def rewrite_pastplace(place_body)
    qid = place_body["uri"][PASTPLACE_PREFIX.length .. -1]
    wikidata_uri = WIKIDATA_PREFIX + qid
    puts "  Rewriting #{place_body["uri"]} -> #{wikidata_uri}"
    place_body["uri"] = wikidata_uri
  end

  # Rewrite fake maphist URIs through the concordance list
  def rewrite_maphist(place_body, quote_body)
    id = parseURI(place_body["uri"])
    match = @concordances.get(id['sorting'], id['line_number'], quote_body["value"])
    if (match.nil?)
      place_body.delete("uri")
      false
    else
      puts "  Rewriting #{place_body["uri"]} -> #{match}"
      place_body["uri"] = match
      true
    end
  end

  def rewrite_one(annotation)
    rewritten = false

    puts "FOOOOOO"
    puts @concordances

    bodies = annotation["bodies"]
    quote_body = getBody(bodies, "QUOTE")
    place_body = getBody(bodies, "PLACE")

    if !quote_body.nil?
      rewrite_quote(quote_body)
    end

    if !place_body.nil?
      uri = place_body["uri"]
      puts "  URI: #{uri}"
      if !uri.nil?
        if uri.start_with?(PASTPLACE_PREFIX)
          rewrite_pastplace(place_body)
          rewritten = true
        elsif uri.start_with?(MAP_HIST_PREFIX)
          rewritten = rewrite_maphist(place_body, quote_body)
        end
      end
    end

    open('out.jsonl', 'a') { |f| f.puts annotation.to_json }

    rewritten
  end

end

WikidataRewriter.new("out-wikidata.jsonl").start()
