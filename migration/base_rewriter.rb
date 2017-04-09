class BaseRewriter

  def initialize(outfile)
    File.delete(outfile) if File.exist?(outfile)
  end

  def getBody(bodies, hasType)
    bodiesOfType = bodies.select do |b|
      b["type"] == hasType
    end

    if bodiesOfType.length > 0
      bodiesOfType[0]
    end
  end

  def start
    if ARGV.empty?
      puts("no input file")
    else
      filename = ARGV[0]
      File.open(filename, "r") do |f|
        ctr = [0, 0]
        f.each_line do |line|
          ctr[0] += 1
          puts "--Record #{ctr[0]} (#{ctr[1]} matches so far)"
          rewritten = rewrite_one(JSON.parse(line))
          if rewritten
            ctr[1] += 1
          end
        end
        puts "#{ctr} URIs rewritten"
      end
    end
  end

end
