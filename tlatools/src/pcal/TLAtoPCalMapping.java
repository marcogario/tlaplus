/**
 */
package pcal;

import java.util.Vector;

import pcal.exception.PcalTLAGenException;

/**
 * A TLA+ to PlusCal mapping is a mapping from regions of the TLA+ translation 
 * to regions of the PlusCal code.  It is used to implement a command that allows the
 * user to jump from a selected region in the TLA+ translation to the region of the
 * PlusCal code that produced that region of the translation, as well as commands
 * to jump directly from the location indicated in an error report from SANY or TLC
 * to the PlusCal code responsible for the error.
 * <p>
 * A TLA+ spec of a TLA+ to PlusCal {@link TLAtoPCalMapping#mapping}  mapping is contained 
 * in the TLAToPCal module that will be appended to the end of this file, but for now lives 
 * in the generals/docs project.
 * <p>
 * The TLAtoPCalMapping object contains a `mapping' field that is the Java implementation
 * of the mapping defined in module TLAToPCal, as well as a method to  
 * <p>
 * TODO to implement this translation:
 * 
 *   Checked calls to the old TLAToken constructor, which required handling
 *   calls from: 
 *       Tokenize.TokenOut 
 *       TLAExpr.substituteAt
 *       PcalTLAGen.SubExpr
 *       PcalTLAGen.selfAsExpr 
 *       PcalTLAGen.GenInit   [modifications deferred]
 *       PcalTLAGen.AddSubscriptsToExpr [modifications deferred]
 *       PcalParams.DefaultVarInit  [nothing done]
 *       PcalFixIDs.FixExpr
 *           
 *   Modified TLAExpr.cloneAndNormalize to preserve the origin.  
 *   Check uses of it to see if something else needs to be done.
 *   The uses are:
 *       PcalTLAGen.SubExpr [looks OK]
 *       PcalTLAGen.GenInit [modifications deferred]
 *       PcalTLAGen.AddSubscriptsToExpr  [modifications deferred]
 *       TLAExpr.substituteAt [modified]
 *       TLAExpr.SeqSubstituteForAll [looks OK]
 *       ParseAlgorithm.SubstituteInStmt  [looks OK]
 *       ParseAlgorithm.SubstituteInSingleAssign  [looks OK]
 *       
 *   Check uses of TLAToken.clone to see that they set the source region
 *   of the new token.
 *       TLAExpr.cloneAndNormalize [fixed to preserve source]
 *       Test.TokVectorToExpr  [just used for testing]
 *       PcalTranslate.TokVectorToExpr 
 *         [Creates the when expression for a pc = ... enabling condition.
 *          Need to do something only if we want the user to be able to
 *          select the label.]
 *       
 *   Check uses of new TLAToken() to see if they need to set the source
 *   region of the new token.  The uses are
 *       Test. ...  : Just for testing.
 *       PcalTranslate.AddedToken [modifications deferred]
 *       PcalTranslate.BuiltInToken [modifications deferred]
 *       PcalTranslate.IdentToken [modifications deferred]
 *       PcalTranslate.StringToken [modifications deferred]
 *   
 *   Modify methods of PcalTranslate to set the origin field of all
 *   AST objects in the parse tree.  Methods modified are:
 *   
 *      GetAssign
 *      GetEither   (++)
 *      GetGoto
 *      GetIf       (++)
 *      GetLHS
 *      GetMacro
 *      GetMacroCall
 *      GetPrintS
 *      GetProcedure 
 *      GetProcess
 *      GetReturn
 *      GetSingleAssign
 *      GetWhile     (++)
 *      getAlgorithm
 *      getAssert
 *      getCall
 *      getCallOrCallReturn
 *      getExpr
 *      GetPVarDecl
 *      getSkip
 *      getWhen 
 *      getWith      (++)
 *      GetWhile
 *      GetVarDecl
 *      SubstituteInLabeledStmt
 *      SubstituteInSingleAssign
 *      ClassifyStmSeq 
 *      SubstituteInStmt  [not really tested]
 *      
 *      todo  ,   
 *      ++ Methods marked with (++) have origin regions that do not include the
 *      "end" (P syntax) or "}" (C syntax) that they should.  See the 
 *      comments to getIf.
 *      
 *      Note: ParseAlgorithm.GetAlgToken and ParseAlgorithm.MustGobbleThis
 *      leave lastTokCol-1 and lastTokLine-1 equal to the (Java)
 *      coordinates of the beginning of the token.  Use GetLastLocationStart
 *      and GetLastLocationEnd to get the PCalLocation of the token.
 *      
 *   Check how missing labels are added, and make sure that use of
 *   GetLastLocationStart/End work properly in that case.  They should
 *   probably return null.  THIS NOW SEEMS TO WORK.
 *   
 *   Modify the PcalFixID class to make sure that origins are properly
 *   set for all the newly created AST nodes.  Have checked and the only
 *   Fix... class that needed modification was FixExpr, which was modified.
 *   
 *   Check all the Explode methods in PcalTranslate.java and modify so that
 *   the newly created AST nodes have the right origin.  Have checked the following:
 *      CopyAndExplodeLastStmt
 *      CopyAndExplodeLastStmtWithGoto
 *      Explode
 *      ExplodeCall
 *      ExplodeCallReturn
 *      ExplodeLabeledStmt
 *      ExplodeLabeledStmtSeq
 *      ExplodeLabelEither
 *      ExplodeLabelIf
 *      ExplodeMultiprocess
 *      ExplodeProcedure
 *      ExplodeProcess
 *      ExplodeReturn
 *      ExplodeUniprocess
 *      ExplodeWhile
 *      UpdatePC 
 *        Adds an assignment to pc with all origins = null.  It might be nice
 *        if the rhs had the origin the region containing the label to which
 *        control is going, but that's probably not worth the effort.
 *  All the Explode methods have been modified appropriately, but not all
 *  have been tested.
 *  
 *  All AST objects obtained by calling the constructors have been checked for
 *  proper setting of their origins.  Calls of the constructors occur only in
 *  ParseAlgorithm and PcalTranslate (in the methods called by Explode).
 *   
 *  Modify PcalTLAGen to generate the actual mapping.  Modified methods:
 *     AddSubscriptsToExpr [not modified]
 *        This adds to the expression the necessary [self] subscripts and primes.
 *        The added tokens have null source region, indicating that they were
 *        added.  For the mapping, the added tokens should be considered part of 
 *        the token that  precedes them.
 *     GenSym [++]
 *     GenUniprocess [++]
 *     GenMultiprocess [++]
 *     GenVarsAndDefs 
 *     GenVarDecl
 *     GenProcSet
 *     GenInit
 *     GenProcess   (Need to modify GenLabeledStmt) 
 *       - This change removed an extra blank line that
 *       - was produced by the old version.
 *     GenProcedure  (Need to modify GenLabeledStmt) 
 *     GenLabeledStmt
 *     GenSkip
 *     GenAssign
 *     GenWhen
 *     GenIf
 *     GenPrintS
 *     GenEither
 *     GenWith
 *     GenAssert
 *     GenNext
 *     GenTermination
 *     GenSpec
 *     
 *     [++] indicates method that's not modified because it just calls other
 *          Gen... methods to produce the translation output.
 *        
 *  Modify PcalTLAGen.getInit and PcalTLAGen.AddSubscriptsToExpr, in which 
 *  no changes have been made yet for adding source regions to constructed tokens.
 *   
 *  METHODS modified:
 *  
 *  TLAExpr.substituteAt : Added BEGIN_REPLACEMENT / END_REPLACEMENT tokens around
 *                 substituted expression.
 *                 
 *  Tokenize.TokenOut : Added the origin to the token that's seems actually to 
 *      produce a token.
 *      
 *  PcalTLAGen.SubExpr: Preserve the origin of the argument's expression in the
 *      value's expression. [Done by making TLAExpr.cloneAndNormalize() do that.]
 *      
 *  PcalTLAGen: Methods for generating AST objects that have been modified:
 *     No constructors of AST objects are called in PcalTLAGen.
 *      
 *  BUGS FOUND AND NOT CORRECTED IN ORIGINAL
 *  - In the construct with (v \in S) {...}, the translation should
 *    put a space before the ":" in "\E v \in S:".
 *    
 *  - The location of the error message generated by
 *  
 *        throw new PcalTLAGenException("Multiple assignment to " ...)
 *        
 *    in PcalTLAGen.GenAssign reports a pretty inaccurate error location.
 *    
 *  - The UNCHANGED line for an individual clause of an either statement
 *    is not wrapped if it's too long.
 *    
 *  - While some wrapping of long lines seems to be done in the fairness 
 *    conjunction of the Spec, I haven't figured out where because it can
 *    produce long lines that could be wrapped--e.g., in the WF and SF clause
 *    for a process with a lot of long ":+" labels.
 *    
 *  TRANSLATION OPTIMIZATIONS:
 *  
 *  - In multiprocess Init conjunct for the pc, remove the CASE if there's
 *    just one process.
 *    
 *  - In GenProcedure and GenProcess, for a process or procedure whose body
 *    consists of just a single labeled statement, can eliminate the 
 *    def
 *    
 *       procName(self) == label(self)
 *    
 *    and use label instead of procName in the definition of Next.
 *    
 *  BUGS NOT YET INVESTIGATED IN TRANSLATION MAPPING
 *  - in tla/pluscal/Bakery.tla, selecting what should map to an entire
 *    statement is mapping to something weird. 
 * @author lamport
 *
 */
public class TLAtoPCalMapping {
  /**
   * The mapping field represents an element of TPMap in the TLAToPCal spec.
   *  
   */
  public MappingObject[][] mapping = new MappingObject[0][] ;
  
  /**
   * This is a version of {@link TLAtoPCalMapping#mapping} as a vector of vectors.
   * It is used while constructing the mapping field, and is then nulled.
   */
  public Vector mappingVector = new Vector(50) ;
  
  public TLAtoPCalMapping() {
      
  }
  
  /**
   * Sets the mapping field to an array version of mapVec, which must be a vector 
   * of vectors of MappingObjects.
   * 
   * @param mapVec
   * @return
   */
  public void  makeMapping(Vector mapVec) {
     this.mapping = new MappingObject[mapVec.size()][] ;
      for (int i = 0 ; i < this.mapping.length; i++) {
          Vector line = (Vector) mapVec.elementAt(i);
          this.mapping[i] = new MappingObject[line.size()];
          for (int j = 0; j < line.size(); j++) {
              this.mapping[i][j] = (MappingObject) line.elementAt(j);
          }
      }
      return ;
  }
  /**
   * Adds a mapping object to mappingVector.
   * 
   * NOT YET USED.  IT SEEMS UNLIKELY THAT IT WILL BE USEFUL.
   * 
   * @param mobj  The object to add.
   * @param line  The line of the translation at which the object
   *              is to be added.  This line is the position relative
   *              to tlaStartLine of the object's location.
   */
//  public void addMappingObject(MappingObject moj, int line) {
//      int nextLine = mappingVector.size() ;
//
//      if (line < nextLine-1) {
//          PcalDebug.ReportBug("Called addMappingObject with line number too small") ;
//      }
//      
//      while (line >= mappingVector.size()) {
//          mappingVector.add(new Vector()) ;
//      }
//      
//      ((Vector) mappingVector.elementAt(line)).addElement(moj) ;
//  }
  
  /**
   * Returns the PCal code location to which `mapping' maps the tpregion Region in the
   * TLA+ translation, where line numbers in `selection' are relative to tlaStartLine.
   * It returns null if the mapping does not map the selection to any PCal code. 
   * This implements the algorithm of module TLAToPCal.
   * <p> 
   * Preconditions: 1. The first and last lines of mapping.mapping are not empty.
   * 
   * @param mapping
   * @param tpregion
   * @return
   */
  public static Region ApplyMapping(TLAtoPCalMapping mapping, Region tpregion) {
      /*
       * This method is the implementation of the algorithm in TLAToPCal.tla.  In that
       * algorithm, a mapping specification is a sequence of tokens.  That sequence
       * corresponds to the concatenation of the lines mapping[0], mapping[1], ...
       * In other words, an index into a specification in the algorithm is implemented
       * by a PCalLocation.  The PCalLocation pcl is the index of the MappingObject
       * mapping.mapping[pcl.line][pcl.column].
       * 
       * However, we modify that spec slightly in the following ways:
       *   - The RegionToTokPair method implements something slightly different
       *     than is specified by the operator of that name in TLAToPCal.
       * 
       */
      
      MappingObject[][] tpMap = mapping.mapping;
//MappingObject.printMapping(map);
      Region tokPairRegion = RegionToTokPair(tpMap, tpregion) ;
      if (tokPairRegion == null) {
          return null;
      }
      
      PCalLocation ltok = tokPairRegion.getBegin();
      PCalLocation rtok = tokPairRegion.getEnd();
      
      /* 
       * The algorithm sets allExpr to true iff tok.inExpr is true for all 
       * tokens from ltok to rtok, where tok.inExpr is true iff token tok
       * comes from an expression.  A TLAToken object contains no information
       * about whether or not it is from an expression, and neither does
       * a MappingObject.SourceToken or MappingObject.Begin/EndTLAToken
       * object.  We can infer whether a MappingObject comes from an
       * expression from the following observations:
       * 
       * - Every SourceToken or Begin/EndToken in an expression comes from
       *   a TLAToken object in a TLAExpr in the field of an AST object.
       * 
       * - The only TLAToken objects in an AST are in a TLAExpr field.
       * 
       * - The preceding observations imply that all SourceToken MappingObjects with
       *   non-null origin come from expressions.  
       *   
       * - The only TLATokens in a TLAExpr that have null origin are added
       *   by PcalTLAGen.addSubscriptsToExpr.  However, all of those added with
       *   null origin lie inside Left/RightParen pairs.  
       *   
       * From all this, we conclude that to determine the entire selection
       * lies within an expression iff every MappingObject at minimum
       * depth is a SourceToken (which I believe represents a TLAToken
       * with non-null origin).
       *                          ------
       * If d is the parenthesis depth of ltok, then set rtokDepth and minDepth 
       * such that d + rtokDepth is the depth of rtok, d + minDepth is the
       * minimum depth of tokens, and allExpr is true iff all tokens in
       * positions ltok to rtok are expression tokens.  However, we take
       * into account that each SourceToken represents in module TLAToPCal
       * a TLAToken enclosed in parentheses.  In computing minDepth and
       * rtokDepth, the only one of those parentheses have to be taken into 
       * consideration are the ones that might lie to the immediate right of
       * ltok and to the immediate left of rtok.
       */
      int rtokDepth = 0;
      int minDepth = 0;
      boolean allExpr = false;
      MappingObject ltokObj = ObjectAt(ltok, tpMap);
      MappingObject rtokObj = ObjectAt(rtok, tpMap);
      if (ltokObj.getType() == MappingObject.SOURCE_TOKEN) {
          allExpr = true;
          minDepth = -1;
          rtokDepth = -1;
      }
      PCalLocation i = NextLocOf(ltok, tpMap);
      while (LTEq(i, rtok)) {
          int newDepth = ModifiedDepth(rtokDepth, i, true, tpMap);
          if (newDepth < minDepth) {
              /*
               * If we've just reached a new minDepth, then token i is
               * a RightParen and we must reset allExpr, which 
               * should reflect only tokens at the minimum depth.
               */
              minDepth = newDepth;
              allExpr = true; 
          }
          else if ((newDepth == rtokDepth) && (newDepth == minDepth)){
              /*
               * We are at minDepth and this token i is not a Paren, so
               * we set allExpr false if token i is not a SourceToken
               */
              allExpr = allExpr && (ObjectAt(i, tpMap).getType() == MappingObject.SOURCE_TOKEN);
          }
          rtokDepth = newDepth;
          i = NextLocOf(i, tpMap);
      }
      
      if (rtokObj.getType() == MappingObject.SOURCE_TOKEN) {
          rtokDepth++;
          if (rtokDepth < minDepth) {
              minDepth = rtokDepth;
          }
      }

      if (allExpr 
              /*
               * The spec has the following conjunction, but it seems to work correctly
               * without it.
               */
             // && ((ltok.getColumn() != rtok.getColumn()) || (ltok.getLine() != rtok.getLine()))
         ){
          minDepth++;
      }
          
      /*
       * Set bParen to first left paren to left of ltok that descends to
       * relative paren depth minDepth.  That left paren can be the implicit
       * left paren before a SourceToken only if that SourceToken is ltok.
       * That is the case iff minDepth is 0, in which case bParen is set to
       * ltok rather than to a LeftParen object.
       */
      int curDepth = 0;
      i = ltok;
      boolean sourceTok = (ltokObj.getType() == MappingObject.SOURCE_TOKEN);

      if ((minDepth != 0) || (!sourceTok)) {
          if (sourceTok) {
              curDepth = -1;
          }
          i = PrevLocOf(ltok, tpMap);
          while (! (   (ObjectAt(i, tpMap).getType() == MappingObject.LEFT_PAREN)
                    && (curDepth == minDepth))){
              curDepth = ModifiedDepth(curDepth, i, false, tpMap);
              i = PrevLocOf(i, tpMap);
          }
      }
      MappingObject bParen = ObjectAt(i, tpMap);
      PCalLocation bParenLoc = i;
      
      /*
       * Set eParen to first right paren to the right of rtok that rises
       * from relative paren depth minDepth.  That is the implicit right
       * paren after a SourceToken iff the SourceToken is rtok and 
       * rtokDepth = minDepth, in which case eParen is set to rtok rather
       * than an RightParen object.
       */
      curDepth = rtokDepth; 
      i = rtok;
      sourceTok = (rtokObj.getType() == MappingObject.SOURCE_TOKEN);
      if ((minDepth != rtokDepth) || (!sourceTok)) {
          if (sourceTok) {
              curDepth--;
          }
          i = NextLocOf(rtok, tpMap);
          while (! (   (ObjectAt(i, tpMap).getType() == MappingObject.RIGHT_PAREN)
                    && (curDepth == minDepth))){
              curDepth = ModifiedDepth(curDepth, i, true, tpMap);
              i = NextLocOf(i, tpMap);
          }
      }
      MappingObject eParen = ObjectAt(i, tpMap);
      PCalLocation eParenLoc = i;
      
      /*
       * The algorithm of TLAToPCal now looks for breaks between bParen and eParen to compute the
       * sequence of disjoint regions that should really be highlighted.  This is not being
       * implemented now, so we skip that part and just construct and return the result.
       */
    
      /*
       * Set lpos and rpos to the PCalLocations marking the left and right of bParen and eParen.
       */
      PCalLocation lpos ;
      if (bParen.getType() == MappingObject.SOURCE_TOKEN) {
          lpos = ((MappingObject.SourceToken) bParen).getOrigin().getBegin();
      }
      else {
          lpos = ((MappingObject.LeftParen) bParen).getLocation();
      }
      
      PCalLocation rpos ;
      if (eParen.getType() == MappingObject.SOURCE_TOKEN) {
          rpos = ((MappingObject.SourceToken) eParen).getOrigin().getEnd();
      }
      else {
          rpos = ((MappingObject.RightParen) eParen).getLocation();
      }
      
      /*
       * If lpos is the location (0,0), then return null.  Else, return (lpos, rpos)
       */
      if (lpos.getLine() == 0 && lpos.getColumn() == 0) {
          return null;
      }
      return new Region(lpos, rpos);
  }
  
  /**
   * If var is the parenthesis depth of the token at location pos, then
   * the following macro sets var to the parenthesis depth of the token
   * at the location past pos if movingForward = TRUE, else the depth at
   * the location before pos if movingForward = FALSE.  This essentially
   * implements the macro of the same name in the algorithm of module
   * TLAToPCal.
   * @return
   */
  private static int ModifiedDepth(int var, PCalLocation pos, boolean movingForward, MappingObject[][] tpMap) {
      int amt = 0;
      int type = ObjectAt(pos, tpMap).getType();
      if (type == MappingObject.LEFT_PAREN) {
          amt = 1;
      }
      else if (type == MappingObject.RIGHT_PAREN) {
          amt = -1;
      }
      return var + (movingForward ? amt : -amt);
  }
  
  /**
   * This implements the operator of the same name in TLAToPCal.tla.  The
   * Region return value is the position in spec of a pair of PCalLocations 
   * of a BeginTLAToken and EndTLAToken, which delimit the region in the
   * TLA+ translation that we interpret the user's selection as having chosen.
   * However, if the user has effectively chosen the entire algorithm, it
   * returns null.
   * 
   * Note: the spec implies that if the right edge of reg is at the beginning of
   * a token, then that token is part of the region returned.  Similarly, if
   * the left edge of reg is at the end of a token, that token is part of the returned
   * region.  This is counterintuitive, and perhaps it should be changed.  However, I
   * will see if this is a practical problem before fixing it.
   * 
   * Preconditions of this method:
   *   The first and last lines of spec are non-empty.
   * @param map
   * @param reg
   * @return
   */
  private static Region RegionToTokPair(MappingObject[][] spec, Region reg) {
      /*
       * Set regBegin, regEnd to the endpoints of Region reg.
       */
      PCalLocation regBegin = reg.getBegin();
      PCalLocation regEnd = reg.getEnd();
      /*
       * If the entire region is outside the translation, return null.
       * If one endpoint is outside the translation, then move it to
       * the beginning or end.
       */
      if (regEnd.getLine() < 0 || regBegin.getLine() >= spec.length) {
          return null;
      }
      
      if (regBegin.getLine() < 0) {
          regBegin = new PCalLocation(0, 0) ;
      }
      
      if (regEnd.getLine() >= spec.length) {
          regEnd = new PCalLocation(spec.length-1,999);
      }
      
      /*
       * Set tokAtOrRightOfBeginning to the location of the first BeginTLAToken 
       * or SourceToken object of 
       * spec such that either regBegin lies within the TLA+ translation region
       * marked by it and its matching EndTLAToken object, of regBegin lies
       * to its left.  Set to null if there is no such BeginTLAToken object.
       * 
       */
      PCalLocation tokAtOrRightOfBeginning = null ;
      
      boolean prevIsBeginToLeft = false;
        // set to true when a BeginTLAToken object is found on line
        // makring a position at or to the left of regBegin.
      boolean notDone = true;
      /*
       * Set locLine to the first line at or after the regBegin.line for
       * which spec has a non-empty line.
       */
      int locLine = regBegin.getLine() ;
      while (locLine < spec.length && spec[locLine].length == 0) {
          locLine++;
      }
      if (locLine >= spec.length) {
          notDone = false;
      }
      PCalLocation loc = new PCalLocation(locLine, 0);
      PCalLocation prevloc = null ;
      
      while (notDone && loc != null) {
          if (prevIsBeginToLeft) {
              MappingObject.EndTLAToken mobj 
                  = (MappingObject.EndTLAToken) ObjectAt(loc, spec) ;
                // This will throw an exception if a BeginTLAToken is not
                // immediately followed by an EndTLAToken
              if (LT(regBegin, new PCalLocation(loc.getLine(), mobj.getColumn()))) {
                  tokAtOrRightOfBeginning = prevloc;
                  notDone = false;
              }
              else {
                  prevIsBeginToLeft = false;
              }
          } 
          else {
              MappingObject obj = ObjectAt(loc, spec);
              if (obj.getType() == MappingObject.END_TLATOKEN) {
                 // If we encounter an EndTLAToken object before a BeginTLAToken
                 // object, then the matching BeginTLAToken object is at an
                 // earlier line than regBeg and is therefore the one we want,
                 // if it is at or to the right of regBegin.
                 MappingObject.EndTLAToken eobj = (MappingObject.EndTLAToken) obj;
                 if (LT(regBegin, new PCalLocation(loc.getLine(), eobj.getColumn()))) {
                     tokAtOrRightOfBeginning = PrevLocOf(loc, spec);
                     notDone = false;
                 }
              }
              else if (obj.getType() == MappingObject.BEGIN_TLATOKEN) {
                  MappingObject.BeginTLAToken bobj = (MappingObject.BeginTLAToken) obj;
                  if (LTEq(regBegin, new PCalLocation(loc.getLine(), bobj.getColumn()))) {
                      tokAtOrRightOfBeginning = loc;
                      notDone = false;
                  }
                  else {
                      prevIsBeginToLeft = true;
                  }
              }
              else if (obj.getType() == MappingObject.SOURCE_TOKEN) {
                  MappingObject.SourceToken sobj = (MappingObject.SourceToken) obj;
                  if (LT(regBegin, new PCalLocation(loc.getLine(), sobj.getEndColumn()))) {
                      tokAtOrRightOfBeginning = loc;
                      notDone = false;
                  }
              }
          }
          prevloc = loc;
          loc = NextLocOf(loc, spec);
      }
      
      /*
       * Set tokAtOrLeftOfEnd to the location of the first EndTLAToken or
       * SourceToken object of spec such that either regEnd lies within 
       * the TLA+ translation region marked by it and its matching EndTLAToken 
       * object, of regBegin lies to its left.  Set to null if there is no 
       * such BeginTLAToken object.  This code is the "mirror image" of
       * the code above for tokAtOrRightOfBegin
       * 
       */

      PCalLocation tokAtOrLeftOfEnd = null ;
      // If regEnd lies within the region of the TLA+ translation delimited 
      // by a BeginTLAToken, EndTLAToken pair, then this is the BeginTLAToken.
      boolean prevIsEndToRight = false;
      // set to true when a BeginTLAToken object is found on line
      // makring a position at or to the left of regBegin.
      notDone = true;
    /*
     * Set locLine to the first line at or after the regEnd.line for
     * which spec has a non-empty line.
     */
    locLine = regEnd.getLine() ;
    while (locLine >= 0 && spec[locLine].length == 0) {
        locLine--;
    }
    if (locLine < 0) {
        notDone = false;
    }
    loc = new PCalLocation(locLine, spec[locLine].length-1);
    prevloc = null ;
    
    while (notDone && loc != null) {
        if (prevIsEndToRight) {
            MappingObject.BeginTLAToken mobj 
                = (MappingObject.BeginTLAToken) ObjectAt(loc, spec) ;
              // This will throw an exception if a BeginTLAToken is not
              // immediately followed by an EndTLAToken
            if (LT(new PCalLocation(loc.getLine(), mobj.getColumn()), regEnd)) {  // should be LT
                tokAtOrLeftOfEnd = prevloc;
                notDone = false;
            }
            else {
                prevIsEndToRight = false;
            }
        } 
        else {
            MappingObject obj = ObjectAt(loc, spec);
            if (obj.getType() == MappingObject.BEGIN_TLATOKEN) {
               // If we encounter a BeginTLAToken object before an EndTLAToken
               // object, then the matching EndTLAToken object is at a
               // later line than regEnd and is therefore the one we want
               // if this BeginTLAToken is at or to the left of regEnd.
               MappingObject.BeginTLAToken eobj = (MappingObject.BeginTLAToken) obj;
               if (LT(new PCalLocation(loc.getLine(), eobj.getColumn()), regEnd)) {
                   tokAtOrLeftOfEnd = NextLocOf(loc, spec);
                   notDone = false;
               }
            }
            else if (obj.getType() == MappingObject.END_TLATOKEN) {
                MappingObject.EndTLAToken bobj = (MappingObject.EndTLAToken) obj;
                if (LTEq(new PCalLocation(loc.getLine(), bobj.getColumn()), regEnd)) {
                    tokAtOrLeftOfEnd = loc;
                    notDone = false;
                }
                else {
                    prevIsEndToRight = true;
                }
            }
            else if (obj.getType() == MappingObject.SOURCE_TOKEN) {
                MappingObject.SourceToken sobj = (MappingObject.SourceToken) obj;
                if (LT(new PCalLocation(loc.getLine(), sobj.getBeginColumn()), regEnd)) {
                    tokAtOrLeftOfEnd = loc;
                    notDone = false;
                }
            }
        }
        prevloc = loc;
        loc = PrevLocOf(loc, spec);
    }
    
    if (tokAtOrRightOfBeginning != null) {
        if (tokAtOrLeftOfEnd == null) {
            return new Region(tokAtOrRightOfBeginning, tokAtOrRightOfBeginning);
        }
        else if (LTEq(tokAtOrRightOfBeginning, tokAtOrLeftOfEnd)) {
            return new Region(tokAtOrRightOfBeginning, tokAtOrLeftOfEnd) ;
        } 
        else {
            /*
             * This is the case in which the region is between TLA+ tokens, so
             * we want to select the token that is closest to that region.  The
             * two possibilities are the one lying to the right, whose beginning
             * is tokAtOrRightOfBeginning, or the one lying to the left,
             * whose end is tokAtOrLeftOfEnd.  We first find their distances
             * to the region.
             */
             int distToLeft;
             MappingObject obj = ObjectAt(tokAtOrLeftOfEnd, spec);
             if (obj.getType() == MappingObject.END_TLATOKEN) {
                 distToLeft = Dist(new PCalLocation (tokAtOrLeftOfEnd.getLine(), 
                                                     ((MappingObject.EndTLAToken) obj).getColumn()),
                                   regBegin);
             }
             else {
                 distToLeft = Dist(new PCalLocation (tokAtOrLeftOfEnd.getLine(), 
                                                      ((MappingObject.SourceToken) obj).getEndColumn()),
                                    regEnd);
             }
             int distToRight;
              obj = ObjectAt(tokAtOrRightOfBeginning, spec);
             if (obj.getType() == MappingObject.BEGIN_TLATOKEN) {
                 distToRight = Dist(new PCalLocation (tokAtOrRightOfBeginning.getLine(), 
                                                     ((MappingObject.BeginTLAToken) obj).getColumn()),
                                    regEnd);
             }
             else {
                 distToRight = Dist(new PCalLocation (tokAtOrRightOfBeginning.getLine(), 
                                                      ((MappingObject.SourceToken) obj).getBeginColumn()),
                                    regEnd);
             }

             /*
              * If needed, we  set leftBegin to be location of the matching 
              * beginning of tokAtOrLeftEnd and rightEnd to be the matching
              * end of tokAtOrRightOfBeginning.
              */
             PCalLocation leftBegin = null;
             PCalLocation rightEnd = null;
             if (distToLeft >= distToRight) {
                  /*
                   * We need rightEnd.
                   */
                  rightEnd = tokAtOrRightOfBeginning;
                  notDone = true ;
                  while (rightEnd != null && notDone) {
                     // rightEnd should never equal null;  we'll get
                     // an exception if it is.  
                     int type = ObjectAt(rightEnd, spec).getType() ;
                     if (   (type == MappingObject.END_TLATOKEN)
                         || (type == MappingObject.SOURCE_TOKEN)) {
                         notDone = false; 
                     }
                     if (notDone) {
                         rightEnd = NextLocOf(rightEnd, spec);
                     }
                 }
             }
             
             if (distToLeft <= distToRight) {
                 /*
                  * We need leftBegin.
                  */
                 leftBegin = tokAtOrLeftOfEnd;
                 notDone = true ;
                 while (notDone) {
                    // leftBegin should never equal null;  we'll get
                    // an exception if it is.
                    int type = ObjectAt(leftBegin, spec).getType() ;
                    if (   (type == MappingObject.BEGIN_TLATOKEN)
                        || (type == MappingObject.SOURCE_TOKEN)) {
                        notDone = false; 
                    }
                    if (notDone) {
                        leftBegin = PrevLocOf(leftBegin, spec);
                    }
                }
            }
            
            if (distToLeft > distToRight) {
                return new Region(tokAtOrRightOfBeginning, rightEnd);
            } 
            else if (distToLeft < distToRight) {
                return new Region(leftBegin, tokAtOrLeftOfEnd);
            }
            else {
                return new Region(leftBegin, rightEnd);
            }
        }
    }
    else if (tokAtOrLeftOfEnd != null) {
        return new Region(tokAtOrLeftOfEnd, tokAtOrLeftOfEnd);
    }
    return null; // both are null, so spec contains no TLA translation tokens.

  }
  
  /**
   * Returns the MappingObject at the location in map indicated by loc.
   * @param loc
   * @param map
   * @return
   */
  private static MappingObject ObjectAt(PCalLocation loc, MappingObject[][] map) {
      return map[loc.getLine()][loc.getColumn()] ;
  }
  
  /**
   * Returns the position within map of the previous object after the one with
   * position loc.  It returns null if there is no previous  object in map.
   * @param loc
   * @param map
   * @return
   */
  private static PCalLocation PrevLocOf(PCalLocation loc, MappingObject[][] map) {
      if (loc.getColumn() > 0) {
          return new PCalLocation(loc.getLine(), loc.getColumn()-1) ;
      }
      for (int i = loc.getLine() - 1; i >= 0; i--) {
          if (map[i].length > 0) {
              return new PCalLocation(i, map[i].length - 1);
          }
      }
      return null;
  }
  
  /**
   * Returns the position within map of the next object after the one with
   * position loc.  It returns null if there is no further object in map.
   * @param loc
   * @param map
   * @return
   */
  private static PCalLocation NextLocOf(PCalLocation loc, MappingObject[][] map) {
      if (loc.getColumn() + 1 < map[loc.getLine()].length) {
          return new PCalLocation(loc.getLine(), loc.getColumn()+1) ;
      }
      for (int i = loc.getLine() + 1; i < map.length; i++) {
          if (map[i].length > 0) {
              return new PCalLocation(i, 0);
          }
      }
      return null;
  }
  
  /**
   * True iff location locA is before or equal to locB
   * @param locA
   * @param locB
   * @return
   */
  private static boolean LTEq(PCalLocation locA, PCalLocation locB) {
      if (locA.getLine() == locB.getLine()) {
          return locA.getColumn() <= locB.getColumn();
      }
      return locA.getLine() < locB.getLine();
  }
 
  /**
   * True iff location locA is before  locB
   * @param locA
   * @param locB
   * @return
   */
  private static boolean LT(PCalLocation locA, PCalLocation locB) {
      if (locA.getLine() == locB.getLine()) {
          return locA.getColumn() < locB.getColumn();
      }
      return locA.getLine() < locB.getLine();
  }

  /**
   * The distance between the two locations, where the distance from the end of 
   * one line to the beginning of the next is considered to be 9999 times as 
   * great as the distance between adjacent characters.
   * 
   * @param locA
   * @param locB
   * @return
   */
  private static int Dist(PCalLocation locA, PCalLocation locB) {
      return 9999 * Math.abs(locA.getLine() - locB.getLine())
              + Math.abs(locA.getColumn() - locB.getColumn());
  }
  
  /* --------------------------------------------------------------------*/
  
  /**
   * The line number of the source file that, when the file was translated,
   * corresponded to line 0 of {@link TLAtoPCalMapping#mapping}.  At that
   * time, it was the line immediately after the BEGIN TRANSLATION comment.
   */
  public int tlaStartLine;
  
  /**
   * The line containing the --algorithm or --fair that begins the
   * algorithm.
   */
  public int algLine ;
  
  /**
   * Should be the column of the beginning --algorithm or --fair that
   * begins the algorithm, but it's actually the column after that
   * token--which should make any difference.
   * 
   * I don't think this is needed.
   */
  public int algColumn ;
  
  /**
   * This removes redundant pairs of matching LeftParen, RightParen objects from
   * the mapping Vector mappingVec.  A redundant LeftParen, RightParen pair is
   * redundant if it comes immediately inside (with no intervening tokens) a matching
   * LeftParen, RightParen pair.  See the RemoveRedundantParens module, which will
   * be inserted as a comment at the end of this file, to see the algorithm on
   * which this implementation is based.  The `in' and `out' sequences of that
   * algorithm are implemented with the Vectors mappingVec and `out' of Vectors,
   * where element indices are represented as PCalLocation objects specifying
   * the "row" and "column" of the object.
   *  
   *
   * 
   * @param mappingVec
   * @return
   */
  public static Vector RemoveRedundantParens(Vector mappingVec) {
      Vector out = new Vector();            // Vector of Vectors of MappingObjects
      Vector unmatchedLeft = new Vector();  
         // Vector of PCalLocations in out of unmatched LeftParen objects
      PCalLocation lastMatchedLeft = null;
        // position in out of last LeftParen object that was matched by
        // a RightParen object
      PCalLocation lastAddedRight = null;
      int i = 0 ;
      while (i < mappingVec.size()) {
          Vector inLine = (Vector) mappingVec.elementAt(i);
          Vector outLine = new Vector();
          out.addElement(outLine);
          int j = 0 ;
          while (j < inLine.size()) {
              MappingObject inObj = (MappingObject) inLine.elementAt(j); 
              if (inObj.getType() == MappingObject.LEFT_PAREN) {
                /*
                 * Bug fix in algorithm.  The positions of the elements of unmatchedLeft
                 * should be their positions in out, not in mappingVec.  The position
                 * in out to which the current LeftParen object is going to be added 
                 * is (i, outLine.size())
                 */
                  unmatchedLeft.addElement(new PCalLocation(i, outLine.size()));
//                unmatchedLeft.addElement(new PCalLocation(i, j));
              }
              else if (inObj.getType() == MappingObject.RIGHT_PAREN) {
                  PCalLocation lastUnmatchedLeft = null ;
                  if (unmatchedLeft.size() != 0) {
                      lastUnmatchedLeft = 
                        (PCalLocation) unmatchedLeft.elementAt(unmatchedLeft.size()-1);
                  }
                  if (   IsNextIn(lastAddedRight, 
                                  new PCalLocation(i, j),
                                  mappingVec)
                      && IsNextIn(lastUnmatchedLeft, lastMatchedLeft,  out)    
                      ) {
                      ((Vector) out.elementAt(lastMatchedLeft.getLine()))
                          .remove(lastMatchedLeft.getColumn());
                      /*
                       * Set lastLine to the last non-empty line of out, then
                       * delete its last element.
                       */
                      Vector lastLine = outLine;
                      int lastLineNum = out.size()-1;
                      while (lastLine.size() == 0) {
                          lastLineNum--;
                          lastLine = (Vector) out.elementAt(lastLineNum);
                      }
                      lastLine.remove(lastLine.size()-1);
                  }
                  lastMatchedLeft = 
                     (PCalLocation) unmatchedLeft.remove(unmatchedLeft.size()-1);
                  lastAddedRight = new PCalLocation(i, j);
              }
              
              outLine.addElement(inObj);
              j++;
          }
          
          i++;
      }
      return out;
  }
  
  /**
   * If vec is a vector of vectors, then it returns true iff locA and locB are
   * both non-null and locA and locB represent consecutive (line, column) locations
   * in vector vec (with locA preceding locB).  If non-null, then the line fields 
   * of locA and locB must both be less than vec.size().
   * 
   * @param locA
   * @param locB
   * @param vec
   * @return
   */
  private static boolean IsNextIn(PCalLocation locA, PCalLocation locB, Vector vec) {
      return     (locA != null)
              && (locB != null)
              && (   (   (locA.getLine() == locB.getLine())
                      && (locA.getColumn()+1 == locB.getColumn())
                     )
                  || (   (locA.getLine() == locB.getLine()-1)
                      && (locA.getColumn() == ((Vector) vec.elementAt(locA.getLine())).size())
                      && (locB.getColumn() == 0)
                     )
                 );
  }
  
}

/* ==================  Begin file TLAToPCal.tla  ========================

----------------------------- MODULE TLAToPCal -----------------------------
EXTENDS Integers, Sequences, TLC

(***************************************************************************)
(* We define the minimum and maximum of a nonempty set of numbers, and the *)
(* absolute value of a number.                                             *)
(***************************************************************************)
Min(S) == CHOOSE i \in S : \A j \in S : i =< j
Max(S) == CHOOSE i \in S : \A j \in S : i >= j
Abs(x) == Max({x, -x})

(***************************************************************************)
(* TP Mapping Specifiers               .                                   *)
(***************************************************************************)
Location == [line : Nat, column : Nat]
  (*************************************************************************)
  (* This is a location in the file, which might be in the TLA+ spec or in *)
  (* the PCal code.                                                        *)
  (*************************************************************************)

loc1 <: loc2 == 
  (*************************************************************************)
  (* This is the "equals or to the left of" relation on locations.         *)
  (*************************************************************************)
  \/ loc1.line < loc2.line
  \/ /\ loc1.line = loc2.line
     /\ loc1.column =< loc2.column

(***************************************************************************)
(* We define Dist(loc1, loc2) to be a natural number representing a        *)
(* distance between locations loc1 and loc2.  This distance is used only   *)
(* to determine which of two other locations a location is closer to.      *)
(* Thus, its magnitude doesn't matter.  I should make Dist a parameter of  *)
(* the spec, but it's less effort to give it some reasonable definition.   *)
(***************************************************************************)
Dist(loc1, loc2) == 
   10000 * Abs(loc2.line - loc1.line) + Abs(loc2.column - loc1.column)
   
Region == {r \in [begin : Location, end : Location] : r.begin <: r.end}
  (*************************************************************************)
  (* This describes a region within the file.  We say that region r1 is to *)
  (* the left of region r2 iff r1.end :< r2.begin                          *)
  (*************************************************************************)

(***************************************************************************)
(* TLA to PCal translation objects.                                        *)
(***************************************************************************)
TLAToken == [type : {"token"}, region  : Region, inExpr : BOOLEAN]
  (*************************************************************************)
  (* This represents a region of tokens in the TLA+ spec, with inExpr      *)
  (* being true iff that region lies within an expression.                 *)
  (*************************************************************************)
Paren    == [type : {"begin", "end"}, loc : Location]
  (*************************************************************************)
  (* This represents the beginning or end of a region in the PlusCal spec. *)
  (*************************************************************************)
Break    == [type : {"break"}, depth : Nat]
  (*************************************************************************)
  (* A Break comes between a right and left Paren at the same parenthesis  *)
  (* level (possibly with TLATokens also between them).  It indicates that *)
  (* there is some PlusCal code between the locations indicated by those   *)
  (* parentheses that should not be displayed when displaying the PlusCal  *)
  (* code for parenthesis levels between the current level lv and lv -     *)
  (* depth.                                                                *)
  (*************************************************************************)
TPObject == TLAToken \cup Paren \cup Break

RECURSIVE ParenDepth(_ , _)
ParenDepth(objSeq, pos) ==
  (*************************************************************************)
  (* Equals the parenthesis depth of the point in the TPObject sequence    *)
  (* objSeq just after element number pos, or at the beginning if pos = 0. *)
  (*************************************************************************)
  IF pos = 0 THEN 0
             ELSE LET obj == objSeq[pos]
                  IN  ParenDepth(objSeq, pos - 1) +
                        ( CASE obj.type = "begin" -> 1  []
                               obj.type = "end"   -> -1 []
                               OTHER              -> 0     )

\*CorrParenDepth(seq, pos) ==
\*  (*************************************************************************)
\*  (* Equals ParenDepth(seq, pos) unless seq[pos] is an expression token,   *)
\*  (* in which case it is one larger.                                       *)
\*  (*************************************************************************)
\*  LET dp ParenDepth(seq, pos)
\*  IN  IF seq[pos].type = "token" /\ seq[pos].inExpr THEN dp +1
\*               
\*                                     ELSE dp
(***************************************************************************)
(* WellFormed(seq) is true for a TPObject sequence iff it begins and ends  *)
(* with a parenthesis and all parentheses are properly matching.           *)
(***************************************************************************)
IsWellFormed(seq) ==  /\ seq # << >>
                      /\ seq[1].type = "begin"
                      /\ \A i \in 1..(Len(seq)-1) : ParenDepth(seq, i) >= 0
                      /\ ParenDepth(seq, Len(seq)) = 0

(***************************************************************************)
(* TokensInOrder(seq) is true for a TPObject sequence iff its TLAToken     *)
(* objects represent regions that are ordered properly--that is, if        *)
(* TLAToken T1 precedes TLAToken T2 in seq, then T1.region is to the left  *)
(* of T2.region.                                                           *)
(***************************************************************************)                      
TokensOf(seq) == {i \in 1..Len(seq) : seq[i].type = "token"}

TokensInOrder(seq) ==
  \A i \in TokensOf(seq) : 
     \A j \in { jj \in TokensOf(seq) : jj > i} : 
        (seq[i].region.end <: seq[j].region.begin)

MatchingParen(seq, pos) ==
  (*************************************************************************)
  (* If element number pos in TPObject sequence seq is a left paren, then  *)
  (* this equals the number n such that element number n is the matching   *)
  (* right paren.                                                          *)
  (*************************************************************************)
  CHOOSE i \in pos+1..Len(seq) :
     /\ ParenDepth(seq,i) = ParenDepth(seq, pos-1)
     /\ \A j \in (pos)..(i-1) : ParenDepth(seq, j) > ParenDepth(seq, pos-1)

(***************************************************************************)
(* A TPMap is a sequence of TPObject elements that has the following       *)
(* interpretation.  The regions of the TLA+ spec contained within its      *)
(* TLAToken elements contain the "important" text of the spec.  Text not   *)
(* in those regions can be treated as if it were white space when          *)
(* determining the PCal region that maps to a part of the TLA+ spec.       *)
(*                                                                         *)
(* Each pair of matching parentheses defines the smallest syntactic unit   *)
(* (e.g., expression or statement) whose translation contains the text in  *)
(* the TLATokens between them.  All the top level (lowest depth)           *)
(* parentheses between those matching parentheses describe successive      *)
(* regions in the same part of the PCal text.  (Code in a macro and code   *)
(* in a procedure is an example of two regions in completely different     *)
(* parts of the PCal code, and hence are not successive regions.) Two      *)
(* successive regions are adjacent if, to higlight both of them, one       *)
(* highlights those regions and the text between them.  If two successive  *)
(* regions represented by two pairs of matching parentheses are not        *)
(* adjacent, then the TPMap contains a Break between them.  The depth of a *)
(* break indicates the number of parenthesis levels containing the break   *)
(* that represent PCal code in which the region between the parenthesized  *)
(* regions on either side of the break should not be highlighted.          *)
(*                                                                         *)
(* The following predicate asserts that seq is a proper TPMap.             *)
(***************************************************************************)
IsTPMap(seq) ==
   (************************************************************************)
   (* There is at least one TLAToken between every matching pair of        *)
   (* parentheses.                                                         *)
   (************************************************************************)
   /\ \A i \in 1..Len(seq) :
         (seq[i].type = "begin") =>
            \E j \in (i+1)..(MatchingParen(seq,i)-1) : seq[j].type = "token"
   (************************************************************************)
   (* A token in an expression is surrounded by parentheses.               *)
   (************************************************************************)
   /\ \A i \in TokensOf(seq) : seq[i].inExpr =>  /\ seq[i-1].type = "begin"
                                                 /\ seq[i+1].type = "end"
   /\ IsWellFormed(seq)
   /\ TokensInOrder(seq)
   (************************************************************************)
   (* The following conjunct asserts that a Break comes between a right    *)
   (* and a left parenthesis at its level, perhaps with intervening        *)
   (* tokens.                                                              *)
   (************************************************************************) 
   /\ \A i \in 1..Len(seq) :
         (seq[i].type = "break") => 
            /\ \E j \in 1..(i-1) : 
                  /\ seq[j].type = "end"
                  /\ \A k \in (j+1)..(i-1) : seq[j].type # "begin"
            /\  \E j \in (i+1)..Len(seq) : 
                  /\ seq[j].type = "begin"
                  /\ \A k \in (i+1)..(j-1) : seq[j].type # "end"
   (************************************************************************)
   (* The following conjunct asserts that matching parentheses have        *)
   (* non-decreasing locations, and that within a pair of matched          *)
   (* parentheses, the regions represented by the top-level matching       *)
   (* parentheses are properly ordered.                                    *)
   (************************************************************************)
   /\ \A i \in 1..Len(seq) :
         (seq[i].type = "begin") =>
            LET j  == MatchingParen(seq, i)
                dp == ParenDepth(seq, i-1) + 1
            IN  /\ seq[i].loc <: seq[j].loc
                /\ \A k \in (i+1)..(j-1) :
                     /\ seq[k].type = "end"
                     /\ ParenDepth(seq, k) = dp
                     => \A m \in (k+1)..(j-1) :
                          /\ seq[m].type = "begin"
                          /\ ParenDepth(seq, m-1) = dp
                          => seq[k].loc <: seq[m].loc

TPMap == {s \in Seq(TPObject) : IsTPMap(s)}           
-----------------------------------------------------------------------------
(***************************************************************************)
(* The Region in the PCal code specified by a Region in the TLA+ spec.     *)
(***************************************************************************)

RegionToTokPair(spec, reg) ==
  (*************************************************************************)
  (* A pair of integers that are the positions of the pair of TLATokens in *)
  (* spec such that they and the TLATokens between them are the ones that  *)
  (* the user has chosen if she has highlighted the region specified by    *)
  (* reg.  (Both tokens could be the same.)                                *)
  (*                                                                       *)
  (* If the region reg does not intersect with the region of any TLAToken  *)
  (* (so it lies entirely inside "white space"), then the value is <<t,    *)
  (* t>> for the token t that lies either to the left or the right of reg. *)
  (*************************************************************************)
  LET TokensContaining(loc) == 
         (******************************************************************)
         (* The set of positions of tokens in spec containing loc.  (It    *)
         (* contains 0 or 1 element.)                                      *)
         (******************************************************************)
         {i \in TokensOf(spec) : /\ spec[i].region.begin <: loc
                                 /\ spec[i].region.begin # loc
                                 /\ loc <: spec[i].region.end
                                 /\ loc # spec[i].region.end  }
                          
      TokensToLeft(loc) == 
        (*******************************************************************)
        (* The set of positions of tokens in spec at or to the left of     *)
        (* loc.                                                            *)
        (*******************************************************************)
        {i \in TokensOf(spec) : spec[i].region.end <: loc}

      TokensToRight(loc) ==
        (*******************************************************************)
        (* The set of positions of tokens in spec at or to the right of    *)
        (* loc.                                                            *)
        (*******************************************************************)
        {i \in TokensOf(spec) : loc <: spec[i].region.begin}

      TokensInRegion == 
        (*******************************************************************)
        (* The set of tokens whose regions lie within reg.                 *)
        (*******************************************************************)
        TokensToRight(reg.begin) \cap TokensToLeft(reg.end)

      S == TokensInRegion \cup TokensContaining(reg.begin) 
            \cup TokensContaining(reg.end)
      
  IN  IF S # {}
        THEN <<Min(S), Max(S)>>
        ELSE LET LeftOfReg  == TokensToLeft(reg.begin)
                 LeftTok == Max(LeftOfReg)
                 RightOfReg == TokensToRight(reg.end)
                 RightTok == Min(RightOfReg)
             IN  CASE LeftOfReg = {}  -> <<RightTok, RightTok>> []
                      RightOfReg = {} -> <<LeftTok, LeftTok>>   []
                      OTHER ->
                        LET dl == Dist(spec[LeftTok].region.end, reg.begin)
                            dr == Dist(spec[RightTok].region.begin, reg.end)
                        IN  CASE dl < dr -> <<LeftTok, LeftTok>>   []
                                 dl > dr -> <<RightTok, RightTok>> []
                                 dl = dr -> <<LeftTok, RightTok>>
               
TokPairToParens(spec, ltok, rtok) ==
  (*************************************************************************)
  (* Assumes ltok and rtok are the positions of TLAToken elements of the   *)
  (* TPMap spec with ltok equal to or to the left of rtok.  It equals the  *)
  (* pair <<lparen, rparen>> where lparen is the position of the           *)
  (* right-most left paren to the left of ltok that enters level dp and    *)
  (* rparen is the position of the left-most right paren to the right of   *)
  (* rtok that leaves level dp, where dp is defined as follows:            *)
  (*                                                                       *)
  (* Let d be the minimum paren depth any token from ltok and rtok.  If    *)
  (* ltok # rtok and every TLAToken element from positions ltok through    *)
  (* rtok is an expression token, then dp = d+1.  Otherwise, dp = d.       *)
  (*************************************************************************)
  LET d == Min ( {ParenDepth(spec, i) : i \in ltok..rtok} )
      dp == IF /\ ltok # rtok 
               /\ \A i \in ltok..rtok : (spec[i].type = "token") =>
                                          spec[i].inExpr 
              THEN d + 1
              ELSE d
      lp == Max ( {i \in 1..ltok : /\ spec[i].type = "begin"
                                   /\ ParenDepth(spec,i) = dp} )
      rp == Min ( {i \in rtok..Len(spec) : /\ spec[i].type = "end"
                                           /\ ParenDepth(spec,i-1) = dp} )
  IN  <<lp, rp>>
-----------------------------------------------------------------------------
(***************************************************************************)
(* For Debugging                                                           *)
(*                                                                         *)
(* To simplify debugging, we assume that locations are all on the same     *)
(* line.                                                                   *)
(***************************************************************************)
Loc(pos) == [line |-> 0, column |-> pos]
Reg(beg, end) == [begin |-> Loc(beg), end |-> Loc(end)]
T(beg, end) == [type |->"token", region |-> Reg(beg, end), inExpr |-> FALSE]
TE(beg, end) == [type |->"token", region |-> Reg(beg, end), inExpr |-> TRUE]
L(pos) == [type |-> "begin", loc |-> Loc(pos)]
R(pos) == [type |-> "end", loc |-> Loc(pos)]
B(dep) == [type |-> "break", depth |-> dep]

tpMap_1 == << L(-5), T(2,3), L(11), T(3, 4), L(12), T(4,5), R(13), 
              T(6,7), R(14), T(8, 9), R(42) >>
tpRegion_1 == Reg(5,20)      
              
tpMap_2 == <<L(10), T(1,2), L(11), T(3,4), L(12), T(5,6), L(13), T(7,8), R(14),
             (* 10 *)T(9,10),  R(15), B(1), L(16), T(11,12), L(17), T(13,14), R(18), 
             (* 18 *) R(19), T(15,16), R(20), R(21)>>
\*        ( lbl : ( (  x[1] := ( 2 + 2 ) ) || y := 3  || ( x[2] := ( 3 ) )  )  )
\*        10     11 12         13     14 15              16        17 18 19 20 21
\*        ( lbl == ( x' = [x EXCEPT ( ![1] = ( 2 + 2 ) , ) ^^ ( ![2] = ( 3 ) ) ] ) )
\*          1-2      3 -----------4   5 ----6  7----8  9-10     11---12  13-14 15-16
tpRegion1 == Reg(0,16)

tpMap1 == << L(1), L(2), TE(1,2), R(3), L(4), TE(2,3), 
             R(5), T(4, 5), L(6), TE(5,6), R(7), R(8)>>

SpecToRegions(spec) ==
  (*************************************************************************)
  (* The set of all regions whose endpoints are in the smallest region     *)
  (* containing all the tokens of spec.                                    *)
  (*************************************************************************)
  LET TT == TokensOf(spec)
      left == Min({spec[i].region.begin.column : i \in TT})
      right == Max({spec[i].region.end.column : i \in TT})
  IN  { Reg(r[1], r[2]) : 
          r \in {rr \in (left..right) \X (left..right) : rr[1] =< rr[2]} }
-----------------------------------------------------------------------------
(***************************************************************************)
(* Declare tpMap to be the TPMap and tpLoc the Location that are the       *)
(* inputs to the algorithm.                                                *)
(***************************************************************************)
CONSTANT tpMap, tpRegion
  
(***************************************************************************
                          The Mapping Algorithm

This algorithm sets the variable `result' to the sequence of regions in
the PCal code that, according to the mapping specification tpMap,
should be highlighted when the user selects the region that is the
value of variable tpregion.  (Variable tpregion is initialized to
tpRegion to test a single region, and to SpecToRegions(tpMap) to test
all subregions.) In the initial Java implementation, I expect that
result will be set to a sequence containing only a single region.
                          
--fair algorithm Map {
    variables  
        tpregion \* = tpRegion ,             
                 \in SpecToRegions(tpMap),  
        ltok,      \* <<ltok, rtok>> is set to 
        rtok,      \*   RegionToTokPair(tpMap, tpregion)
        rtokDepth, \* The paren depth of rtok relative to ltok
        minDepth,  \* The depth of the minimum paren depth TLAToken 
        allExpr,   \* Set to true iff all tokens from ltok to rtok are
                   \*   expression tokens.             
        bParen,    \* <<bParen, eParen>> is set to 
        eParen,    \*   TokPairToParens(tpMap, ltok, rtok)
        result,    \* Set to the sequence of Regions that is the translation.
        curBegin,  \* Used to construct the result
        lastRparen,\*  "    
        i,         \* For loop variable
        curDepth ; \* Temporary variable for holding the paren depth

    (***********************************************************************)
    (* If var is the parenthesis depth of the token at position pos, then  *)
    (* the following macro sets var to the parenthesis depth of the token  *)
    (* at position pos + 1 if movingForward = TRUE, else the depth at pos  *)
    (* - 1 if movingForward = FALSE.                                       *)
    (***********************************************************************)
    macro ModifyDepth(var, pos, movingForward) {
      with (amt = CASE tpMap[pos].type = "begin" ->  1  []
                        tpMap[pos].type = "end"   -> -1 []
                        OTHER                    -> 0     ) {
           var := var + IF movingForward THEN amt ELSE -amt
        }
      }
      
    { with (tp = RegionToTokPair(tpMap, tpregion)) {
         ltok := tp[1];
         rtok := tp[2]
       } ;

      (*********************************************************************)
      (* If d is the depth of ltok, then set rtokDepth and minDepth such   *)
      (* that d + rtokDepth is the depth of rtok, d + minDepth is the      *)
      (* minimum depth of tokens, and allExpr is true iff all tokens in    *)
      (* positions ltok to rtok are expression tokens.                     *)
      (*********************************************************************)
      rtokDepth := 0;
      minDepth := 0 ;
      allExpr := tpMap[ltok].inExpr ;
      i := ltok+1;
      while (i =< rtok) {
         ModifyDepth(rtokDepth, i, TRUE);
         if (rtokDepth < minDepth) {minDepth := rtokDepth} ;
         if (tpMap[i].type = "token") {
           allExpr := allExpr /\ tpMap[i].inExpr
          } ;
         i := i+1
       };
       
      assert /\ ParenDepth(tpMap, rtok) = ParenDepth(tpMap, ltok) + rtokDepth
             /\ minDepth + ParenDepth(tpMap, ltok) =
                  Min({ParenDepth(tpMap, k) : k \in ltok..rtok}) 
             /\ allExpr = \A k \in ltok..rtok : 
                             (tpMap[k].type = "token") => tpMap[k].inExpr ;
                             
      (*********************************************************************)
      (* Increment minDepth if ltok # rtok and allExpr is true.            *)
      (*                                                                   *)
      (* This appears to be an error, because the implementation works     *)
      (* properly without the ltok # rtok conjunct in the `if' test.  I    *)
      (* probably didn't test the spec adequately.  However, it's also     *)
      (* possible that the spec is correct and for some subtle reason, the *)
      (* obvious implementation of the `if' test doesn't actually          *)
      (* implement the spec.  As long as the implementation seems to be    *)
      (* working, I don't want to spend the time figuring out what's going *)
      (* on.                                                               *)
      (*********************************************************************)
      if (ltok # rtok /\ allExpr) {minDepth := minDepth + 1} ;
      
      (*********************************************************************)
      (* Set bParen to first left paren to left of ltok that descends to   *)
      (* relative paren depth minDepth.                                    *)
      (*********************************************************************)
      curDepth := 0;
      i := ltok - 1;
      while (~ /\ tpMap[i].type = "begin"
               /\ curDepth = minDepth ) {
        ModifyDepth(curDepth, i, FALSE) ;
        i := i-1
       } ;
      bParen := i ;
      
      (*********************************************************************)
      (* Set eParen to first right paren to the right of rtok that rises   *)
      (* from relative paren depth minDepth.                               *)
      (*********************************************************************)
      curDepth := rtokDepth;
      i := rtok + 1;
      while (~ /\ tpMap[i].type = "end"
               /\ curDepth = minDepth ) {
        ModifyDepth(curDepth, i, TRUE) ;
        i := i+1
       } ;
      eParen := i ;
      
      assert <<bParen, eParen>> = TokPairToParens(tpMap, ltok, rtok);
      
      (*********************************************************************)
      (* Construct the final result.                                       *)
      (*********************************************************************)
      result := << >> ; 
      curBegin := tpMap[bParen].loc ;
      curDepth := 0 ;
      lastRparen := -1 ;
      i := bParen + 1 ;
      while (i < eParen) {
        if (tpMap[i].type = "end") {
          lastRparen := i
          } ;
        if ( /\ tpMap[i].type = "break"
             /\ tpMap[i].depth - curDepth >= 0 ) {
             assert lastRparen # -1 ;
      
             (**************************************************************)
             (* The following statement will be eliminated in the initial  *)
             (* implementation.                                            *)
             (**************************************************************)
             result := Append(result, 
                              [begin |-> curBegin, end |-> tpMap[lastRparen].loc]);

             lastRparen := -1 ;
             while (tpMap[i].type # "begin") {
               ModifyDepth(curDepth, i, TRUE) ;
               i := i+1;
              } ;
             curBegin := tpMap[i].loc ;         
           } ;
        ModifyDepth(curDepth, i, TRUE) ;
        i := i+1;
       } ;
      result := Append(result, 
                       [begin |-> curBegin, end |-> tpMap[eParen].loc]);
      
      \* debugging output
      print <<tpregion.begin.column, tpregion.end.column>> ;
      print [j \in 1..Len(result) |-> <<result[j].begin.column,
                                        result[j].end.column>>];
      print << "lrtok", ltok, rtok>>
    }
}
 ***************************************************************************)
\* BEGIN TRANSLATION
CONSTANT defaultInitValue
VARIABLES tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, bParen, eParen, 
          result, curBegin, lastRparen, i, curDepth, pc

vars == << tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, bParen, eParen, 
           result, curBegin, lastRparen, i, curDepth, pc >>

Init == (* Global variables *)
        /\ tpregion \in SpecToRegions(tpMap)
        /\ ltok = defaultInitValue
        /\ rtok = defaultInitValue
        /\ rtokDepth = defaultInitValue
        /\ minDepth = defaultInitValue
        /\ allExpr = defaultInitValue
        /\ bParen = defaultInitValue
        /\ eParen = defaultInitValue
        /\ result = defaultInitValue
        /\ curBegin = defaultInitValue
        /\ lastRparen = defaultInitValue
        /\ i = defaultInitValue
        /\ curDepth = defaultInitValue
        /\ pc = "Lbl_1"

Lbl_1 == /\ pc = "Lbl_1"
         /\ LET tp == RegionToTokPair(tpMap, tpregion) IN
              /\ ltok' = tp[1]
              /\ rtok' = tp[2]
         /\ rtokDepth' = 0
         /\ minDepth' = 0
         /\ allExpr' = tpMap[ltok'].inExpr
         /\ i' = ltok'+1
         /\ pc' = "Lbl_2"
         /\ UNCHANGED << tpregion, bParen, eParen, result, curBegin, 
                         lastRparen, curDepth >>

Lbl_2 == /\ pc = "Lbl_2"
         /\ IF i =< rtok
               THEN /\ LET amt == CASE tpMap[i].type = "begin" ->  1  []
                                        tpMap[i].type = "end"   -> -1 []
                                        OTHER                    -> 0 IN
                         rtokDepth' = rtokDepth + IF TRUE THEN amt ELSE -amt
                    /\ IF rtokDepth' < minDepth
                          THEN /\ minDepth' = rtokDepth'
                          ELSE /\ TRUE
                               /\ UNCHANGED minDepth
                    /\ IF tpMap[i].type = "token"
                          THEN /\ allExpr' = (allExpr /\ tpMap[i].inExpr)
                          ELSE /\ TRUE
                               /\ UNCHANGED allExpr
                    /\ i' = i+1
                    /\ pc' = "Lbl_2"
                    /\ UNCHANGED curDepth
               ELSE /\ Assert(/\ ParenDepth(tpMap, rtok) = ParenDepth(tpMap, ltok) + rtokDepth
                              /\ minDepth + ParenDepth(tpMap, ltok) =
                                   Min({ParenDepth(tpMap, k) : k \in ltok..rtok})
                              /\ allExpr = \A k \in ltok..rtok :
                                              (tpMap[k].type = "token") => tpMap[k].inExpr, 
                              "Failure of assertion at line 397, column 7.")
                    /\ IF ltok # rtok /\ allExpr
                          THEN /\ minDepth' = minDepth + 1
                          ELSE /\ TRUE
                               /\ UNCHANGED minDepth
                    /\ curDepth' = 0
                    /\ i' = ltok - 1
                    /\ pc' = "Lbl_3"
                    /\ UNCHANGED << rtokDepth, allExpr >>
         /\ UNCHANGED << tpregion, ltok, rtok, bParen, eParen, result, 
                         curBegin, lastRparen >>

Lbl_3 == /\ pc = "Lbl_3"
         /\ IF ~ /\ tpMap[i].type = "begin"
                 /\ curDepth = minDepth
               THEN /\ LET amt == CASE tpMap[i].type = "begin" ->  1  []
                                        tpMap[i].type = "end"   -> -1 []
                                        OTHER                    -> 0 IN
                         curDepth' = curDepth + IF FALSE THEN amt ELSE -amt
                    /\ i' = i-1
                    /\ pc' = "Lbl_3"
                    /\ UNCHANGED bParen
               ELSE /\ bParen' = i
                    /\ curDepth' = rtokDepth
                    /\ i' = rtok + 1
                    /\ pc' = "Lbl_4"
         /\ UNCHANGED << tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, 
                         eParen, result, curBegin, lastRparen >>

Lbl_4 == /\ pc = "Lbl_4"
         /\ IF ~ /\ tpMap[i].type = "end"
                 /\ curDepth = minDepth
               THEN /\ LET amt == CASE tpMap[i].type = "begin" ->  1  []
                                        tpMap[i].type = "end"   -> -1 []
                                        OTHER                    -> 0 IN
                         curDepth' = curDepth + IF TRUE THEN amt ELSE -amt
                    /\ i' = i+1
                    /\ pc' = "Lbl_4"
                    /\ UNCHANGED << eParen, result, curBegin, lastRparen >>
               ELSE /\ eParen' = i
                    /\ Assert(<<bParen, eParen'>> = TokPairToParens(tpMap, ltok, rtok), 
                              "Failure of assertion at line 434, column 7.")
                    /\ result' = << >>
                    /\ curBegin' = tpMap[bParen].loc
                    /\ curDepth' = 0
                    /\ lastRparen' = -1
                    /\ i' = bParen + 1
                    /\ pc' = "Lbl_5"
         /\ UNCHANGED << tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, 
                         bParen >>

Lbl_5 == /\ pc = "Lbl_5"
         /\ IF i < eParen
               THEN /\ IF tpMap[i].type = "end"
                          THEN /\ lastRparen' = i
                          ELSE /\ TRUE
                               /\ UNCHANGED lastRparen
                    /\ IF /\ tpMap[i].type = "break"
                          /\ tpMap[i].depth - curDepth >= 0
                          THEN /\ Assert(lastRparen' # -1, 
                                         "Failure of assertion at line 450, column 14.")
                               /\ result' = Append(result,
                                                   [begin |-> curBegin, end |-> tpMap[lastRparen'].loc])
                               /\ pc' = "Lbl_6"
                          ELSE /\ pc' = "Lbl_8"
                               /\ UNCHANGED result
               ELSE /\ result' = Append(result,
                                        [begin |-> curBegin, end |-> tpMap[eParen].loc])
                    /\ PrintT(<<tpregion.begin.column, tpregion.end.column>>)
                    /\ PrintT([j \in 1..Len(result') |-> <<result'[j].begin.column,
                                                           result'[j].end.column>>])
                    /\ PrintT(<< "lrtok", ltok, rtok>>)
                    /\ pc' = "Done"
                    /\ UNCHANGED lastRparen
         /\ UNCHANGED << tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, 
                         bParen, eParen, curBegin, i, curDepth >>

Lbl_8 == /\ pc = "Lbl_8"
         /\ LET amt == CASE tpMap[i].type = "begin" ->  1  []
                             tpMap[i].type = "end"   -> -1 []
                             OTHER                    -> 0 IN
              curDepth' = curDepth + IF TRUE THEN amt ELSE -amt
         /\ i' = i+1
         /\ pc' = "Lbl_5"
         /\ UNCHANGED << tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, 
                         bParen, eParen, result, curBegin, lastRparen >>

Lbl_6 == /\ pc = "Lbl_6"
         /\ lastRparen' = -1
         /\ pc' = "Lbl_7"
         /\ UNCHANGED << tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, 
                         bParen, eParen, result, curBegin, i, curDepth >>

Lbl_7 == /\ pc = "Lbl_7"
         /\ IF tpMap[i].type # "begin"
               THEN /\ LET amt == CASE tpMap[i].type = "begin" ->  1  []
                                        tpMap[i].type = "end"   -> -1 []
                                        OTHER                    -> 0 IN
                         curDepth' = curDepth + IF TRUE THEN amt ELSE -amt
                    /\ i' = i+1
                    /\ pc' = "Lbl_7"
                    /\ UNCHANGED curBegin
               ELSE /\ curBegin' = tpMap[i].loc
                    /\ pc' = "Lbl_8"
                    /\ UNCHANGED << i, curDepth >>
         /\ UNCHANGED << tpregion, ltok, rtok, rtokDepth, minDepth, allExpr, 
                         bParen, eParen, result, lastRparen >>

Next == Lbl_1 \/ Lbl_2 \/ Lbl_3 \/ Lbl_4 \/ Lbl_5 \/ Lbl_8 \/ Lbl_6
           \/ Lbl_7
           \/ (* Disjunct to prevent deadlock on termination *)
              (pc = "Done" /\ UNCHANGED vars)

Spec == /\ Init /\ [][Next]_vars
        /\ WF_vars(Next)

Termination == <>(pc = "Done")

\* END TRANSLATION
=============================================================================
\* Modification History
\* Last modified Tue Dec 13 10:12:16 PST 2011 by lamport
\* Created Thu Dec 01 16:51:23 PST 2011 by lamport

 ==================  End file TLAToPCal.tla  ======================== */

/*==================  Begin file RemoveRedundantParens.tla  ================

----------------------- MODULE RemoveRedundantParens -----------------------
(***************************************************************************)
(* Let an expression be a sequence of tokens, some of which are left and   *)
(* right parenthesis tokens, in which parentheses are balanced.  The goal  *)
(* is an algorithm that removes redundant parentheses from the expression. *)
(* A pair of matching left and right parentheses (a( and )b) are redundant *)
(* if they occur immediately inside another pair of matching parentheses,  *)
(* as in                                                                   *)
(*                                                                         *)
(*   (p( (a( xxx (u( xxx )v) xxx )b) )q)                                   *)
(***************************************************************************)
EXTENDS Integers, Sequences, TLC

(***************************************************************************)
(* A Token has a type, which is "left", "right", or "other" and an id,     *)
(* which is an element of the set TokId                                    *)
(***************************************************************************)
CONSTANT TokId
Token == [type : {"left", "right", "other"}, id : TokId]
----------------------------------------------------------------------------
(***************************************************************************)
(* We now define Result(seq) to be the output that the algorithm is        *)
(* supposed to produce.  It's defined recursively to keep removing         *)
(* matching redundant parentheses until there are no left.  We make some   *)
(* preliminary definitions, some of which are used in the algorithm as     *)
(* well.                                                                   *)
(***************************************************************************)

(***************************************************************************)
(* ParenDepth(seq, i) is the parenthesis depth in the expression seq just  *)
(* after the i-th token in seq, where ParenDepth(seq, 0) = 0.              *)
(***************************************************************************)
RECURSIVE ParenDepth(_, _)
ParenDepth(seq, i) == 
  IF i = 0 
    THEN 0  
    ELSE CASE seq[i].type = "left"  -> ParenDepth(seq, i-1) + 1
           [] seq[i].type = "right" -> ParenDepth(seq, i-1) - 1
           [] seq[i].type = "other" -> ParenDepth(seq, i-1)

(***************************************************************************)
(* IsWellFormed(seq) is true iff all parentheses in seq are properly       *)
(* matched.                                                                *)
(***************************************************************************)
IsWellFormed(seq) == /\ \A i \in 1..Len(seq) : ParenDepth(seq, i) >= 0
                     /\ ParenDepth(seq, Len(seq)) = 0
                                
(***************************************************************************)
(* For m < n, this is true iff the m-th and n-th elements of seq are       *)
(* matching parentheses.                                                   *)
(***************************************************************************)
AreMatching(seq, m, n) == /\ seq[m].type = "left"
                          /\ seq[n].type = "right"
                          /\ LET sseq == SubSeq(seq, m, n)
                             IN  /\ ParenDepth(sseq, n-m+1) = 0
                                 /\ \A i \in 1..(n-m) :
                                       ParenDepth(sseq, i) > 0

(***************************************************************************)
(* We now define some useful operators on sequences.                       *)
(***************************************************************************)
RemoveElement(seq, i) == 
  [j \in 1..(Len(seq)-1) |-> IF j < i THEN seq[j] ELSE seq[j+1]]

Last(seq) == seq[Len(seq)]

RemoveLast(seq) == RemoveElement(seq, Len(seq))
                  
RECURSIVE Result(_)
Result(seq) ==
  IF \E r \in (2..(Len(seq)-2)) \X (3..(Len(seq)-1)) : 
       /\ r[1] < r[2]
       /\ AreMatching(seq, r[1], r[2])
       /\ AreMatching(seq, r[1]-1, r[2]+1)
    THEN LET r == 
              CHOOSE r \in (2..(Len(seq)-2)) \X (3..(Len(seq)-1)) : 
               /\ r[1] < r[2]
               /\ AreMatching(seq, r[1], r[2])
               /\ AreMatching(seq, r[1]-1, r[2]+1)
         IN Result(RemoveElement(RemoveElement(seq, r[2]), r[1]))
    ELSE seq
----------------------------------------------------------------------------
(***************************************************************************)
(* The following are some operators useful for writing and debugging the   *)
(* algorithm.  ExprOfMaxLen(n) is the set of all expressions of length at  *)
(* most n.  The algorithm has been checked for all expressions in          *)
(* ExprOfMaxLen(9).  For ExprOfMaxLen(10), there are too many initial      *)
(* states.                                                                 *)
(***************************************************************************)
ExprOfMaxLen(n) == 
  UNION {{s \in [1..i -> Token] : IsWellFormed(s)} : i \in 0..n}

(***************************************************************************)
(* Pr(seq) is a value that, when printed, provides a compact               *)
(* representation of the expression seq.                                   *)
(***************************************************************************)
PrT(tok) == CASE tok.type = "left" -> <<"(", tok.id>> []
                 tok.type = "right" -> <<tok.id, ")">> []
                 tok.type = "other" -> tok.id
     
Pr(seq) == [i \in 1..Len(seq) |-> PrT(seq[i])]            

(***************************************************************************)
(* Some operators for writing expressions compactly.                       *)
(***************************************************************************)
L(i) == [type |-> "left", id |-> i]
R(i) == [type |-> "right", id |-> i]
O(i) == [type |-> "other", id |-> i]
----------------------------------------------------------------------------
(****************************************************************************

--algorithm Remove {
  variables 
    in \in ExprOfMaxLen(9), 
             \* The input.  
     
    out = << >>,           \* The output
    unmatchedLeft = << >>, \* Sequence of indices in `out' of unmatched "("s in out
    lastMatchedLeft = -1,  \* Index in `out' of "(" matched by last ")" in `out'.
                           \* A value of -1 means there is none.
    lastAddedRight = -1,   \* Index in `in' of last ")"  added to `out'.
                           \* A value of -1 means there is none.
    i = 1,                 \* Next element of `in' to examine.
            
 { while (i =< Len(in)) {
      if (in[i].type = "left") {
        unmatchedLeft := Append(unmatchedLeft, Len(out)+1)
      }
      else if (in[i].type = "right") {
        if ( /\ i = lastAddedRight + 1
             /\ lastMatchedLeft = unmatchedLeft[Len(unmatchedLeft)]+1 ) {
          out := RemoveLast(RemoveElement(out, lastMatchedLeft));
        };
        lastMatchedLeft := Last(unmatchedLeft);
        unmatchedLeft := RemoveLast(unmatchedLeft);
        lastAddedRight := i
      } ;
      out := Append(out, in[i]);
      i := i+1;
    };
    \* If out is not the correct result, print in, out and the
    \* correct result and report an error.
    if ( out # Result(in)) {
       print <<"in:", Pr(in)>>;
       print <<"out;", Pr(out)>>;
       print <<"res:",Pr(Result(in))>>;
       assert FALSE
    } 
 }

}
****************************************************************************)
\* BEGIN TRANSLATION
VARIABLES in, out, unmatchedLeft, lastMatchedLeft, lastAddedRight, i, pc

vars == << in, out, unmatchedLeft, lastMatchedLeft, lastAddedRight, i, pc >>

Init == (* Global variables *)
        /\ in \in ExprOfMaxLen(10)
        /\ out = << >>
        /\ unmatchedLeft = << >>
        /\ lastMatchedLeft = -1
        /\ lastAddedRight = -1
        /\ i = 1
        /\ pc = "Lbl_1"

Lbl_1 == /\ pc = "Lbl_1"
         /\ IF i =< Len(in)
               THEN /\ IF in[i].type = "left"
                          THEN /\ unmatchedLeft' = Append(unmatchedLeft, Len(out)+1)
                               /\ UNCHANGED << out, lastMatchedLeft, 
                                               lastAddedRight >>
                          ELSE /\ IF in[i].type = "right"
                                     THEN /\ IF /\ i = lastAddedRight + 1
                                                /\ lastMatchedLeft = unmatchedLeft[Len(unmatchedLeft)]+1
                                                THEN /\ out' = RemoveLast(RemoveElement(out, lastMatchedLeft))
                                                ELSE /\ TRUE
                                                     /\ out' = out
                                          /\ lastMatchedLeft' = Last(unmatchedLeft)
                                          /\ unmatchedLeft' = RemoveLast(unmatchedLeft)
                                          /\ lastAddedRight' = i
                                     ELSE /\ TRUE
                                          /\ UNCHANGED << out, unmatchedLeft, 
                                                          lastMatchedLeft, 
                                                          lastAddedRight >>
                    /\ pc' = "Lbl_2"
               ELSE /\ IF out # Result(in)
                          THEN /\ PrintT(<<"in:", Pr(in)>>)
                               /\ PrintT(<<"out;", Pr(out)>>)
                               /\ PrintT(<<"res:",Pr(Result(in))>>)
                               /\ Assert(FALSE, 
                                         "Failure of assertion at line 136, column 8.")
                          ELSE /\ TRUE
                    /\ pc' = "Done"
                    /\ UNCHANGED << out, unmatchedLeft, lastMatchedLeft, 
                                    lastAddedRight >>
         /\ UNCHANGED << in, i >>

Lbl_2 == /\ pc = "Lbl_2"
         /\ out' = Append(out, in[i])
         /\ i' = i+1
         /\ pc' = "Lbl_1"
         /\ UNCHANGED << in, unmatchedLeft, lastMatchedLeft, lastAddedRight >>

Next == Lbl_1 \/ Lbl_2
           \/ (* Disjunct to prevent deadlock on termination *)
              (pc = "Done" /\ UNCHANGED vars)

Spec == Init /\ [][Next]_vars

Termination == <>(pc = "Done")

\* END TRANSLATION

=============================================================================
\* Modification History
\* Last modified Tue Dec 20 15:29:06 PST 2011 by lamport
\* Created Mon Dec 19 17:20:10 PST 2011 by lamport

=========================  End file RemoveRedundantParens.tla  =======================*/
