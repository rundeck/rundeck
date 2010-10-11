' Word2HTML.vbs
' $Id: word2html.vbs 4 2005-09-01 19:52:40Z connary_scott $
Option Explicit

' Creates the output directories to the same depth as the inputFile specified
' param inBaseDir         The base directory for input files
' param inputFile         The file being processed
' param outBaseDir        The base directory for output files
Function CreateOutputDirectories(inBaseDir, inputFile, outBaseDir)
  Dim relativePath
  relativePath = Mid(inputFile, Len(inBaseDir)+1)
  
  ' ensure the path doesn't end with a \
  If Left(relativePath, 1) = "\" Then
      relativePath = Right(relativePath, Len(relativePath) - 1)
  End If
  
  ' ensure the input basedir doesn't end with a \
  If Right(inBaseDir, 1) = "\" Then
      baseDir = Left(inBaseDir, Len(inBaseDir) - 1)
  End If
  
  ' ensure the output basedir doesn't end with a \
  If Right(outBaseDir, 1) = "\" Then
      outBaseDir = Left(outBaseDir, Len(outBaseDir) - 1)
  End If
  
  Dim index, folder, paths, Files
  paths = Split(relativePath, "\")
  folder = outBaseDir
  Set Files = WScript.CreateObject("Scripting.FileSystemObject")
  For index = LBound(paths) to UBound(paths) -1
      folder = folder & "\" & paths(index)
      If Not Files.FolderExists(folder) Then
      	Files.CreateFolder(folder)
      End If
  Next 
  
  CreateOutputDirectories = folder
End Function

' Main chunk of code
Dim basedir, FileSys, HTMLFormat, inputFile, obj, outputDir, outputFile

HTMLFormat = 8
' Fully Qualified File name
inputFile=WScript.Arguments(0)
' Directory to place results in
outputDir=WScript.Arguments(1)
' base directory for the input file
basedir = WScript.Arguments(2)
' work out the directory structure for the input file
Set FileSys = WScript.CreateObject("Scripting.FileSystemObject")

outputFile = CreateOutputDirectories(basedir, inputFile, outputDir) & "\" & _
    FileSys.GetBaseName(inputFile) & ".html"

Set obj = WScript.CreateObject("Word.Application")

obj.Visible = FALSE
obj.Documents.Open(inputFile)
obj.ActiveDocument.SaveAs outputFile, HTMLFormat
obj.Quit
