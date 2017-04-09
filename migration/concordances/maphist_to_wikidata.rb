require 'csv'

class MaphistToWikidata

  @@MAP_HIST_PREFIX = "http://www.maphistory.info/portolans/record/"

  def initialize()
    @concordances = {}

    puts "Loading Maphist-Wikidata concordances"

    csv_text = File.read("./concordances/data/maphist_to_wikidata.csv")
    csv = CSV.parse(csv_text, :headers => true, :col_sep => ';')
    csv.each do |row|
      maphist_id          = row[0].split('-')
      maphist_sorting     = maphist_id[1]
      maphist_line_number = maphist_id[0]
      toponym             = row[1]
      mapped_uri          = row[2]
      status              = row[6]

      if status == "VERIFIED" && !mapped_uri.empty?
        addRecord(maphist_sorting, maphist_line_number, toponym, mapped_uri)
      end
    end

    puts "Done"

    loadCorrections()
  end

  private def addRecord(sorting, line_number, toponym, mapped_uri)
    record = {
      "sorting"     => sorting,
      "line_number" => line_number,
      "mapped_uri"  => mapped_uri,
      "toponym"     => toponym
    }

    existing_records = @concordances[sorting]
    if (existing_records.nil?)
      @concordances[sorting] = [record]
    else
      existing_records << record
    end
  end

  private def loadCorrections
    puts "Loading previous corrections"
    if File.exist?("concordances/data/maphist_wikidata_corrections.csv")
      csv_text = File.read("concordances/data/maphist_wikidata_corrections.csv")
      csv = CSV.parse(csv_text, :headers => false, :col_sep => ';')
      csv.each { |row| addRecord(row[0], row[1], row[2], row[3]) }
    end
    puts "Done"
  end

  private def storeCorrection(r, corrected_line)
    addRecord(r["sorting"], corrected_line, r["toponym"], r["mapped_uri"])
    open('concordances/data/maphist_wikidata_corrections.csv', 'a') do |f|
      f.puts "#{r["sorting"]};#{corrected_line};#{r["toponym"]};#{r["mapped_uri"]}"
    end
  end

  def get(sorting, line_number, toponym)
    matches = @concordances[sorting]
    if (matches.nil?)
      puts "  No concordance found for #{line_number}-#{sorting}"
    elsif matches.length == 1
      offset = matches[0]["line_number"].to_i - line_number.to_i
      puts "  One concordance found (offset #{offset})"
      matches[0]["mapped_uri"]
    else
      puts "  Multiple possible matches for #{toponym}:"

      maybeExact = matches.select do |m|
        m["line_number"] == line_number
      end

      if maybeExact.empty?
        # No exact match - query user
        matches.each_with_index do |match, idx|
          offset = match["line_number"].to_i - line_number.to_i
          puts "  [#{idx}] #{match['line_number']} #{match['toponym']}, offset #{offset}"
        end

        selection = $stdin.gets.chomp.to_i
        puts "  Returning selection #{selection}"
        storeCorrection(matches[selection], line_number)
        matches[selection]["mapped_uri"]
      else
        # Exact match
        puts "  Exact line number match"
        maybeExact[0]["mapped_uri"]
      end

    end
  end

end
