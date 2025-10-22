#!/usr/bin/env python3
"""
Script to generate a list of deployed pull requests to production between specified dates.
Usage: python list_deployed_prs.py --start-date 2024-11-01 --end-date 2025-10-03
"""

import argparse
import requests
import sys
from datetime import datetime
from typing import List, Dict, Any

GITHUB_API_BASE = "https://api.github.com"
REPO_OWNER = "rundeck"
REPO_NAME = "rundeck"


def parse_date(date_str: str) -> datetime:
    """Parse date string in YYYY-MM-DD format."""
    return datetime.strptime(date_str, "%Y-%m-%d")


def get_releases(start_date: datetime, end_date: datetime) -> List[Dict[str, Any]]:
    """Get releases from GitHub within the specified date range."""
    url = f"{GITHUB_API_BASE}/repos/{REPO_OWNER}/{REPO_NAME}/releases"
    params = {"per_page": 100}
    
    all_releases = []
    page = 1
    
    while True:
        params["page"] = page
        response = requests.get(url, params=params)
        
        if response.status_code != 200:
            print(f"Error fetching releases: {response.status_code}", file=sys.stderr)
            print(f"Response: {response.text}", file=sys.stderr)
            break
        
        releases = response.json()
        if not releases:
            break
        
        for release in releases:
            if release.get("draft", False):
                continue
            
            published_at_str = release.get("published_at")
            if not published_at_str:
                continue
            
            published_at = datetime.strptime(published_at_str, "%Y-%m-%dT%H:%M:%SZ")
            
            if start_date <= published_at <= end_date:
                all_releases.append(release)
        
        page += 1
        if len(releases) < 100:
            break
    
    return sorted(all_releases, key=lambda r: r["published_at"])


def get_commits_between_tags(previous_tag: str, current_tag: str) -> List[Dict[str, Any]]:
    """Get commits between two tags."""
    url = f"{GITHUB_API_BASE}/repos/{REPO_OWNER}/{REPO_NAME}/compare/{previous_tag}...{current_tag}"
    
    response = requests.get(url)
    
    if response.status_code != 200:
        print(f"Error comparing tags {previous_tag}...{current_tag}: {response.status_code}", file=sys.stderr)
        return []
    
    data = response.json()
    return data.get("commits", [])


def extract_pr_number(commit_message: str) -> int:
    """Extract PR number from commit message."""
    import re
    # Look for patterns like (#1234) or Merge pull request #1234
    patterns = [
        r'#(\d+)',
        r'Merge pull request #(\d+)',
        r'\(#(\d+)\)'
    ]
    
    for pattern in patterns:
        match = re.search(pattern, commit_message)
        if match:
            return int(match.group(1))
    
    return None


def get_pr_details(pr_number: int) -> Dict[str, Any]:
    """Get details for a specific pull request."""
    url = f"{GITHUB_API_BASE}/repos/{REPO_OWNER}/{REPO_NAME}/pulls/{pr_number}"
    
    response = requests.get(url)
    
    if response.status_code != 200:
        return None
    
    return response.json()


def main():
    parser = argparse.ArgumentParser(
        description="Generate list of deployed pull requests between specified dates"
    )
    parser.add_argument(
        "--start-date",
        required=True,
        help="Start date in YYYY-MM-DD format (e.g., 2024-11-01)"
    )
    parser.add_argument(
        "--end-date",
        required=True,
        help="End date in YYYY-MM-DD format (e.g., 2025-10-03)"
    )
    parser.add_argument(
        "--output",
        default="deployed_prs.md",
        help="Output file name (default: deployed_prs.md)"
    )
    
    args = parser.parse_args()
    
    try:
        start_date = parse_date(args.start_date)
        end_date = parse_date(args.end_date)
    except ValueError as e:
        print(f"Error parsing dates: {e}", file=sys.stderr)
        sys.exit(1)
    
    print(f"Fetching releases between {start_date.date()} and {end_date.date()}...")
    releases = get_releases(start_date, end_date)
    
    if not releases:
        print("No releases found in the specified date range.")
        sys.exit(0)
    
    print(f"Found {len(releases)} releases in the specified date range.")
    
    output_lines = []
    output_lines.append(f"# Deployed Pull Requests to Production\n")
    output_lines.append(f"**Date Range:** {start_date.date()} to {end_date.date()}\n")
    output_lines.append(f"**Repository:** {REPO_OWNER}/{REPO_NAME}\n")
    output_lines.append(f"**Total Releases:** {len(releases)}\n")
    output_lines.append("\n---\n")
    
    all_prs = set()
    
    for i, release in enumerate(releases):
        tag_name = release["tag_name"]
        release_name = release["name"]
        published_at = release["published_at"]
        html_url = release["html_url"]
        
        print(f"\nProcessing release: {release_name} ({tag_name})")
        
        output_lines.append(f"\n## {release_name}\n")
        output_lines.append(f"- **Tag:** {tag_name}\n")
        output_lines.append(f"- **Published:** {published_at}\n")
        output_lines.append(f"- **URL:** {html_url}\n")
        
        # Get previous release tag
        previous_tag = None
        if i > 0:
            previous_tag = releases[i - 1]["tag_name"]
        else:
            # For the first release in range, try to get the previous release before the date range
            all_tags_url = f"{GITHUB_API_BASE}/repos/{REPO_OWNER}/{REPO_NAME}/tags"
            response = requests.get(all_tags_url, params={"per_page": 100})
            if response.status_code == 200:
                all_tags = response.json()
                current_idx = next((idx for idx, t in enumerate(all_tags) if t["name"] == tag_name), None)
                if current_idx is not None and current_idx + 1 < len(all_tags):
                    previous_tag = all_tags[current_idx + 1]["name"]
        
        if previous_tag:
            print(f"  Comparing {previous_tag}...{tag_name}")
            commits = get_commits_between_tags(previous_tag, tag_name)
            
            release_prs = set()
            for commit in commits:
                message = commit.get("commit", {}).get("message", "")
                pr_number = extract_pr_number(message)
                if pr_number:
                    release_prs.add(pr_number)
                    all_prs.add(pr_number)
            
            if release_prs:
                output_lines.append(f"\n### Pull Requests ({len(release_prs)})\n")
                for pr_num in sorted(release_prs):
                    pr_url = f"https://github.com/{REPO_OWNER}/{REPO_NAME}/pull/{pr_num}"
                    output_lines.append(f"- [#{pr_num}]({pr_url})\n")
                    
                    # Optionally get PR details (commented out to avoid rate limiting)
                    # pr_details = get_pr_details(pr_num)
                    # if pr_details:
                    #     output_lines.append(f"  - **Title:** {pr_details['title']}\n")
                    #     output_lines.append(f"  - **Author:** {pr_details['user']['login']}\n")
            else:
                output_lines.append(f"\n*No pull requests found for this release.*\n")
        else:
            output_lines.append(f"\n*Cannot determine previous release for comparison.*\n")
        
        output_lines.append("\n---\n")
    
    # Summary
    output_lines.append(f"\n## Summary\n")
    output_lines.append(f"- **Total Releases:** {len(releases)}\n")
    output_lines.append(f"- **Total Unique Pull Requests:** {len(all_prs)}\n")
    
    # Write to file
    with open(args.output, 'w') as f:
        f.writelines(output_lines)
    
    print(f"\n✓ Report generated: {args.output}")
    print(f"  Total releases: {len(releases)}")
    print(f"  Total unique PRs: {len(all_prs)}")


if __name__ == "__main__":
    main()
