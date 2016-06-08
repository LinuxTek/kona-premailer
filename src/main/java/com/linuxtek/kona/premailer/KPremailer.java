/*
 * Copyright (C) 2014 LinuxTek, Inc.  All Rights Reserved.
 */
package com.linuxtek.kona.premailer;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.linuxtek.kona.io.KScript;
import com.linuxtek.kona.io.KScriptException;
import com.linuxtek.kona.util.KFileUtil;

/**
 * <p>Inlines CSS using the Premailer Ruby library.</p>
 * https://github.com/premailer/premailer
 * - assumes you have at least Ruby 1.9.3 installed and the following gems:
 * -    gem install premailer
 * -    gem install hpricot
 * 
 * - NOTE installing hpricot will require gcc and ruby-devel to be installed on the server
 */
public class KPremailer {
	private static Logger logger = Logger.getLogger(KPremailer.class);

	public static final String[] PATHS = {"/usr/bin/ruby", "/usr/local/bin/ruby"};
    
	public static final String SCRIPT = ""
        + "require 'rubygems'\n"
        + "require 'premailer'\n\n"
        + "premailer = Premailer.new('%s', :warn_level => Premailer::Warnings::SAFE)\n\n"
        + "File.open('%s', 'w') do |fout|\n"
        + "   fout.puts premailer.to_inline_css\n"
        + "end\n\n"
        + "premailer.warnings.each do |w|\n"
        + "   puts \"#{w[:message]} (#{w[:level]}) may not render properly in #{w[:clients]}\"\n"
        + "end\n\n";

    public static String processHtml(String html) throws IOException {
    	File in = KFileUtil.writeTempFile(html);
        File out = KFileUtil.createTempFile();
        String infile = in.getAbsolutePath();
        String outfile = out.getAbsolutePath();
        String script = String.format(SCRIPT, infile, outfile);
        logger.debug("KPremailer: generated Ruby script:\n" + script);
        
        File scriptFile = KFileUtil.writeTempFile(script);
        String ruby = null;
        for (String path : PATHS) {
        	File f = new File(path);
        	if (f.exists()) {
        		ruby = path;
        	}
        }
        
        if (ruby == null) {
        	throw new IOException("Ruby executable not found on this system.");
        }
        
        try {
        	KScript proc = new KScript(ruby, scriptFile.getAbsolutePath());
			proc.run();
		} catch (KScriptException e) {
            throw new IOException(e);
		}
        return KFileUtil.readFile(outfile);
    }
}
